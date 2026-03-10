package com.milkteabot.service;

import com.milkteabot.model.MenuItem;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MenuService {

    private final List<MenuItem> menuItems = new ArrayList<>();

    @PostConstruct
    public void loadMenu() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("/Menu.csv")))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                String[] parts = line.split(",");
                if (parts.length < 7) continue;

                menuItems.add(new MenuItem(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        Integer.parseInt(parts[4].trim()),
                        Integer.parseInt(parts[5].trim()),
                        Boolean.parseBoolean(parts[6].trim())
                ));
            }
            log.info("Loaded {} menu items", menuItems.size());

        } catch (Exception e) {
            log.error("Failed to load menu: {}", e.getMessage());
        }
    }

    public List<MenuItem> getAll() {
        return menuItems.stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.toList());
    }

    public Map<String, List<MenuItem>> getGroupedByCategory() {
        return menuItems.stream()
                .filter(MenuItem::isAvailable)
                .collect(Collectors.groupingBy(MenuItem::getCategory,
                        LinkedHashMap::new, Collectors.toList()));
    }

    public Optional<MenuItem> findById(String itemId) {
        return menuItems.stream()
                .filter(i -> i.getItemId().equalsIgnoreCase(itemId))
                .findFirst();
    }

    public Optional<MenuItem> findByName(String name) {
        return menuItems.stream()
                .filter(i -> i.getName().equalsIgnoreCase(name))
                .findFirst();
    }
}
