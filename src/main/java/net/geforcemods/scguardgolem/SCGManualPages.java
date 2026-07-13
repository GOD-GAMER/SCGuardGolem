package net.geforcemods.scguardgolem;

import net.geforcemods.securitycraft.items.SCManualItem;
import net.geforcemods.securitycraft.misc.PageGroup;
import net.geforcemods.securitycraft.misc.SCManualPage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
//? if >=1.21.8 {
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
//?} elif forge {
/*import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
*///?} else
/*import net.neoforged.fml.event.lifecycle.InterModProcessEvent;*/

/**
 * Registers the guard golem's guide as a NATIVE SecurityCraft manual page, so it
 * appears inside SecurityCraft's own Manual styled like every other entry (no
 * hardcoded written book).
 *
 * <p>The registration hook differs by era, and SCGuardGolem wires the right one at
 * construction: SecurityCraft populates + syncs {@code SCManualItem.PAGES} via
 * {@code OnDatapackSyncEvent} on MC 1.21.8+ (PAGES is cleared/rebuilt server-side
 * and pushed to clients), and via {@code InterModProcessEvent} on the pre-1.21.8
 * line (built once per side, never cleared).
 */
public final class SCGManualPages {
    private SCGManualPages() {}

    /** The golem's page: a 6-field record on {@literal <=}1.21.1, 7-field (recipe supplier) on 1.21.8+. */
    public static SCManualPage buildPage() {
        Item icon = SCGContent.SCG_MANUAL.get();
        Component title = Component.translatable("scguardgolem.manual.title");
        Component help = Component.translatable("scguardgolem.manual.help");
        //? if >=1.21.8 {
        Supplier<Optional<List<RecipeDisplay>>> noRecipe = () -> Optional.empty();
        return new SCManualPage(icon, PageGroup.NONE, title, help, "SCGuardGolem Team", false, noRecipe);
        //?} else
        /*return new SCManualPage(icon, PageGroup.NONE, title, help, "SCGuardGolem Team", false);*/
    }

    private static void addPageIfAbsent() {
        Item icon = SCGContent.SCG_MANUAL.get();
        if (SCManualItem.PAGES.stream().noneMatch(p -> p.item() == icon))
            SCManualItem.PAGES.add(buildPage());
    }

    // 1.21.8+ : add server-side at HIGH priority (before SC's NORMAL sync sender), deduped.
    //? if >=1.21.8 {
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        addPageIfAbsent();
    }

    public static EventPriority syncPriority() {
        return EventPriority.HIGH;
    }
    //?} else {
    /*public static void onInterModProcess(InterModProcessEvent event) {
        addPageIfAbsent();
    }
    *///?}
}
