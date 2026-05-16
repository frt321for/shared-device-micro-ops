package com.iot.ops.application.config;

import com.iot.ops.application.module.auth.domain.User;
import com.iot.ops.application.module.auth.repository.UserRepository;
import com.iot.ops.application.module.device.domain.Device;
import com.iot.ops.application.module.device.domain.DeviceType;
import com.iot.ops.application.module.device.repository.DeviceRepository;
import com.iot.ops.application.module.device.repository.DeviceTypeRepository;
import com.iot.ops.application.module.inventory.domain.DeviceStock;
import com.iot.ops.application.module.inventory.domain.Sku;
import com.iot.ops.application.module.inventory.repository.DeviceStockRepository;
import com.iot.ops.application.module.inventory.repository.SkuRepository;
import com.iot.ops.application.module.site.domain.Site;
import com.iot.ops.application.module.site.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteRepository siteRepository;
    private final DeviceTypeRepository deviceTypeRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceStockRepository deviceStockRepository;
    private final SkuRepository skuRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already initialized, skipping");
            return;
        }

        log.info("Initializing dev test data...");

        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("password"));
        admin.setDisplayName("系统管理员");
        admin.setRole("admin");
        userRepository.save(admin);

        User manager = new User();
        manager.setUsername("manager");
        manager.setPasswordHash(passwordEncoder.encode("password"));
        manager.setDisplayName("运维经理");
        manager.setRole("manager");
        userRepository.save(manager);

        User tech = new User();
        tech.setUsername("tech");
        tech.setPasswordHash(passwordEncoder.encode("password"));
        tech.setDisplayName("运维工程师");
        tech.setRole("tech");
        userRepository.save(tech);

        User replenisher = new User();
        replenisher.setUsername("replenisher");
        replenisher.setPasswordHash(passwordEncoder.encode("password"));
        replenisher.setDisplayName("补货员");
        replenisher.setRole("replenisher");
        userRepository.save(replenisher);

        User maintainer = new User();
        maintainer.setUsername("maintainer");
        maintainer.setPasswordHash(passwordEncoder.encode("password"));
        maintainer.setDisplayName("维修员");
        maintainer.setRole("maintainer");
        userRepository.save(maintainer);

        User warehouse = new User();
        warehouse.setUsername("warehouse");
        warehouse.setPasswordHash(passwordEncoder.encode("password"));
        warehouse.setDisplayName("仓管员");
        warehouse.setRole("warehouse_keeper");
        userRepository.save(warehouse);

        Site site1 = new Site();
        site1.setName("天安云谷一期");
        site1.setAddress("深圳市龙岗区坂田街道天安云谷");
        site1.setBuilding("1栋A座");
        site1.setServiceLevel("standard");
        site1.setStatus("active");
        site1.setContactName("张经理");
        site1.setContactPhone("13800138001");
        site1.setLatitude(Double.parseDouble("22.6551"));
        site1.setLongitude(Double.parseDouble("114.0642"));
        siteRepository.save(site1);

        Site site2 = new Site();
        site2.setName("华强北赛格广场");
        site2.setAddress("深圳市福田区华强北路");
        site2.setBuilding("赛格广场B座");
        site2.setServiceLevel("premium");
        site2.setStatus("active");
        site2.setContactName("李主管");
        site2.setContactPhone("13800138002");
        site2.setLatitude(Double.parseDouble("22.5411"));
        site2.setLongitude(Double.parseDouble("114.0835"));
        siteRepository.save(site2);

        Site site3 = new Site();
        site3.setName("科技园南区");
        site3.setAddress("深圳市南山区科技南路");
        site3.setBuilding("软件产业基地");
        site3.setServiceLevel("basic");
        site3.setStatus("active");
        site3.setContactName("王工");
        site3.setContactPhone("13800138003");
        site3.setLatitude(Double.parseDouble("22.5175"));
        site3.setLongitude(Double.parseDouble("113.9504"));
        siteRepository.save(site3);

        DeviceType dt1 = new DeviceType();
        dt1.setCode("vending_stand");
        dt1.setName("立式售货机");
        dt1.setCategory("vending");
        deviceTypeRepository.save(dt1);

        DeviceType dt2 = new DeviceType();
        dt2.setCode("vending_desk");
        dt2.setName("台式售货机");
        dt2.setCategory("vending");
        deviceTypeRepository.save(dt2);

        DeviceType dt3 = new DeviceType();
        dt3.setCode("interactive");
        dt3.setName("互动广告屏");
        dt3.setCategory("interactive");
        deviceTypeRepository.save(dt3);

        Sku sku1 = new Sku();
        sku1.setCode("COLA-500");
        sku1.setName("可口可乐 500ml");
        sku1.setCategory("beverage");
        sku1.setUnit("瓶");
        sku1.setCostPrice(new BigDecimal("2.50"));
        sku1.setSellingPrice(new BigDecimal("4.00"));
        sku1.setReorderPoint(50);
        skuRepository.save(sku1);

        Sku sku2 = new Sku();
        sku2.setCode("SPRITE-500");
        sku2.setName("雪碧 500ml");
        sku2.setCategory("beverage");
        sku2.setUnit("瓶");
        sku2.setCostPrice(new BigDecimal("2.30"));
        sku2.setSellingPrice(new BigDecimal("4.00"));
        sku2.setReorderPoint(50);
        skuRepository.save(sku2);

        Sku sku3 = new Sku();
        sku3.setCode("WATER-550");
        sku3.setName("农夫山泉 550ml");
        sku3.setCategory("beverage");
        sku3.setUnit("瓶");
        sku3.setCostPrice(new BigDecimal("1.20"));
        sku3.setSellingPrice(new BigDecimal("2.50"));
        sku3.setReorderPoint(80);
        skuRepository.save(sku3);

        for (int i = 1; i <= 5; i++) {
            Device d = new Device();
            d.setDeviceCode("DEV-A" + String.format("%03d", i));
            d.setDeviceTypeId(dt1.getId());
            d.setName("立式售货机-A" + String.format("%03d", i));
            d.setSiteId(site1.getId());
            d.setStatus(i % 2 == 0 ? "online" : "offline");
            d.setCapacity(100);
            deviceRepository.save(d);

            DeviceStock stock = new DeviceStock();
            stock.setDeviceId(d.getId());
            stock.setSkuId(1L);
            stock.setQuantity(i == 1 ? 0 : i * 10);
            stock.setMinThreshold(20);
            stock.setMaxCapacity(200);
            stock.setStatus("active");
            deviceStockRepository.save(stock);
        }

        log.info("Dev test data initialized: users=3, sites=3, deviceTypes=3, devices=5");
    }
}
