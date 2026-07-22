package com.sanhiruzu.ami.compat;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftablesServiceTest {
    @Test
    void stopsAtFirstHandledProvider() {
        AtomicInteger calls = new AtomicInteger();
        List<ItemStack> expected = List.of(ItemStack.EMPTY);
        CraftablesService service = new CraftablesService(List.of(
                () -> CraftablesProvider.Result.handled(expected),
                () -> {
                    calls.incrementAndGet();
                    return CraftablesProvider.Result.handled(List.of());
                }
        ));

        List<ItemStack> result = service.collectCraftables();

        assertEquals(expected, result);
        assertEquals(0, calls.get());
    }

    @Test
    void fallsThroughUnhandledProviders() {
        AtomicInteger calls = new AtomicInteger();
        List<ItemStack> expected = List.of(ItemStack.EMPTY);
        CraftablesService service = new CraftablesService(List.of(
                CraftablesProvider.Result::unhandled,
                () -> {
                    calls.incrementAndGet();
                    return CraftablesProvider.Result.handled(expected);
                }
        ));

        List<ItemStack> result = service.collectCraftables();

        assertEquals(expected, result);
        assertEquals(1, calls.get());
    }

    @Test
    void handledEmptyResultIsAuthoritative() {
        AtomicInteger calls = new AtomicInteger();
        CraftablesService service = new CraftablesService(List.of(
                () -> CraftablesProvider.Result.handled(List.of()),
                () -> {
                    calls.incrementAndGet();
                    return CraftablesProvider.Result.handled(List.of(ItemStack.EMPTY));
                }
        ));

        assertEquals(List.of(), service.collectCraftables());
        assertEquals(0, calls.get());
    }
}
