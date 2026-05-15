package com.iot.ops.application.module.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.device.repository.DeviceTypeRepository;
import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DeviceSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);
    private static final Random RANDOM = new Random();
    private static final String[] SKU_NAMES = {"矿泉水", "可乐", "咖啡", "薯片", "饼干", "巧克力", "纸巾"};
    private static final int[] SKU_PRICES = {300, 500, 800, 600, 400, 1200, 200};

    private final SiteRepository siteRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final SkuRepository skuRepository;
    private final MqttEventPublisher eventPublisher;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private static class SimDevice {
        Device device;
        Map<Long, Integer> stocks = new HashMap<>();
        int heartbeatCount = 0;

        SimDevice(Device device, List<Sku> skus) {
            this.device = device;
            for (Sku sku : skus) {
                stocks.put(sku.getId(), 30 + RANDOM.nextInt(70));
            }
        }
    }

    private final List<SimDevice> simDevices = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<Site> sites = siteRepository.findAll();
        if (sites.isEmpty()) {
            log.info("No sites found, skipping auto-simulator init");
            return;
        }
        List<DeviceType> types = deviceTypeRepository.findAll();
        List<Sku> skus = skuRepository.findAll();
        if (types.isEmpty() || skus.isEmpty()) {
            log.info("No device types or SKUs found, skipping simulator");
            return;
        }

        for (Site site : sites) {
            long existingCount = deviceRepository.findBySiteId(site.getId()).size();
            int devicesToCreate = 2 - (int) existingCount;
            if (devicesToCreate <= 0) continue;

            boolean hasVending = existingCount > 0;
            for (int i = 0; i < devicesToCreate; i++) {
                DeviceType type = hasVending ? types.get(RANDOM.nextInt(types.size())) : types.get(0);
                hasVending = true;
                String code = "SIM-" + type.getCode().toUpperCase() + "-" + site.getId() + "-" + (existingCount + i + 1);
                Device device = Device.builder()
                    .deviceCode(code)
                    .deviceTypeId(type.getId())
                    .name(site.getName() + "-" + type.getName() + "-" + (existingCount + i + 1))
                    .siteId(site.getId())
                    .status("online")
                    .capacity(100)
                    .build();
                device = deviceRepository.save(device);
                simDevices.add(new SimDevice(device, skus));
                log.info("Created simulator device: {} at site {}", code, site.getName());
            }
        }

        for (Device dev : deviceRepository.findAll()) {
            boolean found = simDevices.stream().anyMatch(s -> s.device.getId().equals(dev.getId()));
            if (!found) {
                simDevices.add(new SimDevice(dev, skus));
            }
        }

        if (!simDevices.isEmpty()) {
            start();
        }
    }

    public void start() {
        if (running) return;
        running = true;
        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::reportHeartbeats, 2, 15, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::reportInventoryAndTransactions, 5, 20, TimeUnit.SECONDS);
        log.info("Device simulator started with {} devices", simDevices.size());
    }

    public boolean isRunning() {
        return running;
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (scheduler != null) scheduler.shutdown();
    }

    private void reportHeartbeats() {
        for (SimDevice sd : simDevices) {
            sd.heartbeatCount++;
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", LocalDateTime.now().toString());
            data.put("uptime", sd.heartbeatCount * 15 + "s");
            data.put("temperature", 22 + RANDOM.nextDouble() * 10);
            eventPublisher.publish(sd.device.getDeviceCode(), "heartbeat", data);

            if (sd.heartbeatCount % 20 == 0 && RANDOM.nextDouble() < 0.15) {
                Map<String, Object> fault = new HashMap<>();
                fault.put("faultCode", "temperature");
                fault.put("severity", "medium");
                fault.put("description", "Temperature spike detected: " + (38 + RANDOM.nextInt(5)) + "C");
                eventPublisher.publish(sd.device.getDeviceCode(), "fault", fault);
            }
        }
    }

    private void reportInventoryAndTransactions() {
        for (SimDevice sd : simDevices) {
            for (Map.Entry<Long, Integer> entry : sd.stocks.entrySet()) {
                int consumption = RANDOM.nextInt(4);
                int newQty = Math.max(0, entry.getValue() - consumption);
                entry.setValue(newQty);

                Map<String, Object> data = new HashMap<>();
                data.put("skuId", entry.getKey());
                data.put("quantity", newQty);
                data.put("loss", consumption > 0 && RANDOM.nextDouble() < 0.05 ? 1 : 0);
                eventPublisher.publish(sd.device.getDeviceCode(), "inventory", data);

                if (consumption > 0 && RANDOM.nextDouble() < 0.6) {
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("skuId", entry.getKey());
                    tx.put("quantity", consumption);
                    tx.put("amount", consumption * (RANDOM.nextInt(10) + 5));
                    tx.put("payMethod", RANDOM.nextBoolean() ? "wechat" : "alipay");
                    eventPublisher.publish(sd.device.getDeviceCode(), "transaction", tx);
                }
            }
        }
    }
}
