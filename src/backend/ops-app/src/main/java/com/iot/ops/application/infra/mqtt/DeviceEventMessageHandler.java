package com.iot.ops.application.infra.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ops.application.infra.cache.DeviceCache;
import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.domain.StockLossRecord;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.inventory.repository.StockLossRecordRepository;
import com.iot.ops.application.module.revenue.domain.OrderEvent;
import com.iot.ops.application.module.revenue.repository.OrderEventRepository;
import com.iot.ops.application.module.device.domain.DeviceTelemetry;
import com.iot.ops.application.module.device.domain.DeviceEvent;
import com.iot.ops.application.module.device.repository.TelemetryRepository;
import com.iot.ops.application.module.device.repository.DeviceEventRepository;
import com.iot.ops.application.module.workorder.domain.WorkOrder;
import com.iot.ops.application.module.workorder.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class DeviceEventMessageHandler implements MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(DeviceEventMessageHandler.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, PreviousStock> previousStockMap = new ConcurrentHashMap<>(64);

    private final DeviceRepository deviceRepository;
    private final DeviceStockRepository deviceStockRepository;
    private final StockLossRecordRepository stockLossRecordRepository;
    private final OrderEventRepository orderEventRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DeviceCache deviceCache;
    private final TelemetryRepository telemetryRepository;
    private final DeviceEventRepository deviceEventRepository;

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        try {
            String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
            String payload = message.getPayload().toString();
            if (topic == null || payload == null) return;

            String[] parts = topic.split("/");
            if (parts.length < 4) return;
            String deviceCode = parts[2];
            String eventType = parts[3];

            Optional<Device> deviceOpt = deviceRepository.findByDeviceCode(deviceCode);
            if (deviceOpt.isEmpty()) {
                log.warn("Unknown device: {}", deviceCode);
                return;
            }
            Device device = deviceOpt.get();

            @SuppressWarnings("unchecked")
            Map<String, Object> data = mapper.readValue(payload, Map.class);

            switch (eventType) {
                case "heartbeat" -> handleHeartbeat(device, data);
                case "inventory" -> handleInventory(device, data);
                case "fault" -> handleFault(device, data);
                case "transaction" -> handleTransaction(device, data);
                default -> log.debug("Unknown event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process MQTT message: {}", e.getMessage());
        }
    }

    private void handleHeartbeat(Device device, Map<String, Object> data) {
        LocalDateTime now = LocalDateTime.now();
        device.setLastHeartbeat(now);
        device.transitionTo("online");
        deviceRepository.save(device);
        deviceCache.updateHeartbeat(device.getDeviceCode(), now);

        if (data.containsKey("temperature")) {
            double temp = ((Number) data.get("temperature")).doubleValue();
            DeviceTelemetry t = DeviceTelemetry.builder()
                .time(Instant.now())
                .deviceId(device.getId())
                .metric("temperature")
                .value(temp)
                .tags(null)
                .build();
            telemetryRepository.save(t);
        }

        DeviceTelemetry hb = DeviceTelemetry.builder()
            .time(Instant.now())
            .deviceId(device.getId())
            .metric("heartbeat")
            .value(1.0)
            .tags(null)
            .build();
        telemetryRepository.save(hb);
    }

    private void handleInventory(Device device, Map<String, Object> data) {
        Number skuIdNum = (Number) data.get("skuId");
        Number quantityNum = (Number) data.get("quantity");
        if (skuIdNum == null || quantityNum == null) return;

        Long skuId = skuIdNum.longValue();
        int quantity = quantityNum.intValue();

        Optional<DeviceStock> stockOpt = deviceStockRepository.findByDeviceIdAndSkuId(device.getId(), skuId);
        DeviceStock stock;
        if (stockOpt.isPresent()) {
            stock = stockOpt.get();
            stock.setQuantity(quantity);
        } else {
            stock = DeviceStock.builder()
                .deviceId(device.getId())
                .skuId(skuId)
                .quantity(quantity)
                .minThreshold(10)
                .maxCapacity(100)
                .build();
        }

        if (quantity <= 0) {
            stock.setStatus("out_of_stock");
        } else if (quantity <= stock.getMinThreshold()) {
            stock.setStatus("low_stock");
            tryAutoCreateWorkOrder(device, stock);
        } else {
            stock.setStatus("adequate");
        }

        if (stock.getQuantity() > 0 && stock.getMinThreshold() > 0) {
            int currentQty = stock.getQuantity();
            String key = device.getId() + ":" + skuId;
            PreviousStock prev = previousStockMap.get(key);
            double hourlyRate;
            if (prev != null && prev.quantity() > currentQty) {
                long elapsedHours = java.time.Duration.between(prev.time(), LocalDateTime.now()).toHours();
                if (elapsedHours > 0) {
                    int delta = prev.quantity() - currentQty;
                    hourlyRate = (double) delta / elapsedHours;
                } else {
                    hourlyRate = 1.0 + Math.random() * 2.0;
                }
            } else {
                hourlyRate = 1.0 + Math.random() * 2.0;
            }
            if (previousStockMap.size() < 10000) {
                previousStockMap.put(key, new PreviousStock(currentQty, LocalDateTime.now()));
            }
            if (hourlyRate > 0) {
                double hoursUntilEmpty = currentQty / hourlyRate;
                stock.setPredictedSoldOut(LocalDateTime.now().plusHours((long) Math.ceil(hoursUntilEmpty)));
            }
        }

        deviceStockRepository.save(stock);

        deviceCache.updateStock(device.getDeviceCode(), skuId, quantity);

        DeviceTelemetry inv = DeviceTelemetry.builder()
            .time(Instant.now())
            .deviceId(device.getId())
            .metric("inventory_" + skuId)
            .value((double) quantity)
            .tags(null)
            .build();
        telemetryRepository.save(inv);

        DeviceEvent event = DeviceEvent.builder()
            .deviceId(device.getId())
            .eventType("inventory_change")
            .eventData("{\"skuId\":" + skuId + ",\"quantity\":" + quantity + "}")
            .severity("info")
            .occurredAt(LocalDateTime.now())
            .build();
        deviceEventRepository.save(event);

        if (data.containsKey("loss") && ((Number) data.get("loss")).intValue() > 0) {
            int loss = ((Number) data.get("loss")).intValue();
            StockLossRecord lossRecord = StockLossRecord.builder()
                .deviceId(device.getId())
                .skuId(skuId)
                .quantity(loss)
                .reason("spoilage")
                .description("Auto-reported inventory loss")
                .build();
            stockLossRecordRepository.save(lossRecord);
        }
    }

    private void handleFault(Device device, Map<String, Object> data) {
        device.transitionTo("fault");
        deviceRepository.save(device);

        String faultCode = (String) data.getOrDefault("faultCode", "unknown");
        String description = (String) data.getOrDefault("description", "");
        deviceCache.recordFault(device.getDeviceCode(), faultCode);

        String severity = (String) data.getOrDefault("severity", "medium");
        String title = switch (faultCode) {
            case "offline" -> device.getName() + " 离线";
            case "jammed" -> device.getName() + " 卡货";
            case "empty" -> device.getName() + " 缺料";
            case "temperature" -> device.getName() + " 温控异常";
            case "door" -> device.getName() + " 门锁异常";
            case "payment" -> device.getName() + " 支付异常";
            default -> device.getName() + " 故障(" + faultCode + ")";
        };

        int priority = switch (severity) {
            case "critical" -> 3;
            case "high" -> 2;
            default -> 1;
        };

        WorkOrder wo = WorkOrder.builder()
            .orderNo(generateOrderNo())
            .type("repair")
            .status("pending_assign")
            .priority(priority)
            .deviceId(device.getId())
            .siteId(device.getSiteId())
            .title(title)
            .description(description)
            .build();
        workOrderRepository.save(wo);
        log.info("Auto-created repair work order {} for device {} fault: {}", wo.getOrderNo(), device.getDeviceCode(), faultCode);
    }

    private void handleTransaction(Device device, Map<String, Object> data) {
        Number skuIdNum = (Number) data.get("skuId");
        Number qtyNum = (Number) data.get("quantity");
        Number amountNum = (Number) data.get("amount");
        if (skuIdNum == null || qtyNum == null) return;

        OrderEvent event = OrderEvent.builder()
            .deviceId(device.getId())
            .siteId(device.getSiteId())
            .skuId(skuIdNum.longValue())
            .quantity(qtyNum.intValue())
            .amount(amountNum != null ? BigDecimal.valueOf(amountNum.doubleValue()) : BigDecimal.ZERO)
            .payMethod((String) data.getOrDefault("payMethod", "wechat"))
            .status("completed")
            .eventTime(LocalDateTime.now())
            .build();
        orderEventRepository.save(event);
    }

    private void tryAutoCreateWorkOrder(Device device, DeviceStock stock) {
        boolean hasPending = workOrderRepository.findByDeviceIdAndStatus(device.getId(), "pending_assign")
            .stream().anyMatch(wo -> "replenishment".equals(wo.getType()));
        if (!hasPending) {
            WorkOrder wo = WorkOrder.builder()
                .orderNo(generateOrderNo())
                .type("replenishment")
                .status("pending_assign")
                .priority(2)
                .deviceId(device.getId())
                .siteId(device.getSiteId())
                .skuId(stock.getSkuId())
                .title(device.getName() + " 缺货预警")
                .description("库存余 " + stock.getQuantity() + "，低于阈值 " + stock.getMinThreshold())
                .expectedQty(stock.getMaxCapacity() - stock.getQuantity())
                .build();
            workOrderRepository.save(wo);
            log.info("Auto-created replenishment work order {} for device {}", wo.getOrderNo(), device.getDeviceCode());
        }
    }

    private record PreviousStock(int quantity, LocalDateTime time) {}

    private String generateOrderNo() {
        return "WO" + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
            + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
