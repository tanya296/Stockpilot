package com.stockpilot.config;

import com.stockpilot.entity.*;
import com.stockpilot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fills an empty database with demo data so the app is usable right away.
 * CommandLineRunner.run() executes once after the application starts.
 * Disable with app.seed.enabled=false (e.g. in tests).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockMovementRepository movementRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }
        log.info("Seeding demo data");

        user("admin@stockpilot.dev", "Asha Admin", Role.ADMIN);
        user("manager@stockpilot.dev", "Manoj Manager", Role.MANAGER);
        user("staff@stockpilot.dev", "Sita Staff", Role.STAFF);

        Warehouse chennai = warehouse("Chennai Depot", "Chennai");
        Warehouse pune = warehouse("Pune Depot", "Pune");
        Warehouse delhi = warehouse("Delhi Depot", "Delhi");

        Product brakePads = product("BRK-PAD-01", "Front Brake Pads", "Brakes", "1850.00", 40);
        Product brakeDisc = product("BRK-DSC-02", "Brake Disc Rotor", "Brakes", "3200.00", 15);
        Product oilFilter = product("ENG-OIL-03", "Oil Filter", "Engine", "450.00", 60);
        Product sparkPlug = product("ENG-SPK-04", "Iridium Spark Plug", "Engine", "690.00", 80);
        Product wiper = product("BOD-WIP-05", "Wiper Blade 22in", "Body", "520.00", 30);
        Product headlamp = product("ELE-HDL-06", "LED Headlamp Bulb", "Electrical", "1250.00", 20);

        // Brake pads: low in Chennai, surplus in Pune: the classic transfer scenario.
        stock(brakePads, chennai, 6);
        stock(brakePads, pune, 70);
        stock(brakePads, delhi, 12);
        stock(brakeDisc, chennai, 10);
        stock(brakeDisc, delhi, 3);
        stock(oilFilter, chennai, 120);
        stock(oilFilter, pune, 45);
        stock(sparkPlug, pune, 30);
        stock(sparkPlug, delhi, 25);
        stock(wiper, chennai, 40);
        stock(headlamp, delhi, 8);

        for (String[] s : List.of(
                new String[]{"Bosch India", "orders@bosch.example"},
                new String[]{"Brembo Distributors", "sales@brembo.example"},
                new String[]{"Minda Auto Parts", "supply@minda.example"})) {
            Supplier supplier = new Supplier();
            supplier.setName(s[0]);
            supplier.setContact(s[1]);
            supplierRepository.save(supplier);
        }
    }

    private void user(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode("Password@123"));
        userRepository.save(u);
    }

    private Warehouse warehouse(String name, String city) {
        Warehouse w = new Warehouse();
        w.setName(name);
        w.setCity(city);
        return warehouseRepository.save(w);
    }

    private Product product(String sku, String name, String category, String price, int reorderLevel) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setUnitPrice(new BigDecimal(price));
        p.setReorderLevel(reorderLevel);
        return productRepository.save(p);
    }

    // Opening stock is recorded as a movement too, so the ledger explains every unit.
    private void stock(Product product, Warehouse warehouse, int qty) {
        StockLevel level = new StockLevel(product, warehouse);
        level.setQuantity(qty);
        stockLevelRepository.save(level);
        movementRepository.save(new StockMovement(product, warehouse, MovementType.IN, qty,
                "Opening stock", "system"));
    }
}
