package com.sanhiruzu.ami.compat;

final class VanillaCraftablesProvider implements CraftablesProvider {
    @Override
    public Result getCraftables() {
        return Result.handled(VanillaCraftablesService.getCraftables());
    }
}
