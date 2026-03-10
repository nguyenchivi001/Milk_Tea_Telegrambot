package com.milkteabot.service;

import com.milkteabot.model.MenuItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MenuServiceTest {

    private MenuService menuService;

    @BeforeEach
    void setUp() {
        menuService = new MenuService();
        menuService.loadMenu();
    }

    // ─────────────────────────────────────────────
    // loadMenu / getAll
    // ─────────────────────────────────────────────

    @Test
    void loadMenu_shouldLoadItems() {
        assertThat(menuService.getAll()).isNotEmpty();
    }

    @Test
    void getAll_shouldOnlyReturnAvailableItems() {
        assertThat(menuService.getAll()).allMatch(MenuItem::isAvailable);
    }

    // ─────────────────────────────────────────────
    // findById
    // ─────────────────────────────────────────────

    @Test
    void findById_shouldReturnCorrectItem() {
        Optional<MenuItem> item = menuService.findById("TS01");

        assertThat(item).isPresent();
        assertThat(item.get().getName()).isEqualTo("Trà Sữa Trân Châu Đen");
        assertThat(item.get().getPriceM()).isEqualTo(35000);
        assertThat(item.get().getPriceL()).isEqualTo(45000);
        assertThat(item.get().getCategory()).isEqualTo("Trà Sữa");
    }

    @Test
    void findById_shouldBeCaseInsensitive() {
        assertThat(menuService.findById("ts01")).isPresent();
        assertThat(menuService.findById("Ts01")).isPresent();
    }

    @Test
    void findById_shouldReturnEmptyForNonExistentId() {
        assertThat(menuService.findById("INVALID_ID")).isEmpty();
    }

    // ─────────────────────────────────────────────
    // findByName
    // ─────────────────────────────────────────────

    @Test
    void findByName_shouldReturnCorrectItem() {
        Optional<MenuItem> item = menuService.findByName("Trà Sữa Trân Châu Đen");

        assertThat(item).isPresent();
        assertThat(item.get().getItemId()).isEqualTo("TS01");
    }

    @Test
    void findByName_shouldReturnEmptyForNonExistentName() {
        assertThat(menuService.findByName("Món Không Tồn Tại")).isEmpty();
    }

    // ─────────────────────────────────────────────
    // getGroupedByCategory
    // ─────────────────────────────────────────────

    @Test
    void getGroupedByCategory_shouldContainExpectedCategories() {
        Map<String, List<MenuItem>> grouped = menuService.getGroupedByCategory();

        assertThat(grouped).containsKeys("Trà Sữa", "Cà Phê", "Trà Trái Cây", "Đá Xay");
    }

    @Test
    void getGroupedByCategory_shouldGroupItemsCorrectly() {
        Map<String, List<MenuItem>> grouped = menuService.getGroupedByCategory();

        assertThat(grouped.get("Trà Sữa"))
                .allMatch(i -> i.getCategory().equals("Trà Sữa"));
        assertThat(grouped.get("Cà Phê"))
                .allMatch(i -> i.getCategory().equals("Cà Phê"));
    }
}
