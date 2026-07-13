package net.geforcemods.scguardgolem;

import java.util.ArrayList;
import java.util.List;

import net.geforcemods.securitycraft.items.SCManualItem;
import net.geforcemods.securitycraft.misc.PageGroup;
import net.geforcemods.securitycraft.misc.SCManualPage;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
//? if >=1.21.8 {
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
 * Registers the guard golem's guide as NATIVE SecurityCraft manual pages — one
 * standalone {@link PageGroup#NONE} page per topic, so the guide reads like a
 * real in-game manual rather than a single wall of text.
 *
 * <p>SecurityCraft dedups/keys manual pages by their icon {@code Item}, so every
 * topic uses a distinct vanilla or addon item as its icon (never an SC item that
 * already owns a page). Registration hook is era-split exactly as before:
 * {@code OnDatapackSyncEvent}@HIGH on 1.21.8+ (PAGES is cleared/rebuilt + synced),
 * {@code InterModProcessEvent} (deferred via {@code enqueueWork} — that phase is
 * parallel and SC writes the same static list) on the pre-1.21.8 line.
 */
public final class SCGManualPages {
    private SCGManualPages() {}

    /** One guide topic: its icon (also the dedup key + in-world object) and its lang keys. */
    private record Topic(Item icon, String titleKey, String helpKey) {}

    private static List<Topic> topics() {
        return List.of(
            new Topic(SCGContent.SCG_MANUAL.get(), "scguardgolem.manual.overview.title",  "scguardgolem.manual.overview.help"),
            new Topic(Items.CARVED_PUMPKIN,        "scguardgolem.manual.convert.title",   "scguardgolem.manual.convert.help"),
            new Topic(Items.COMPARATOR,            "scguardgolem.manual.config.title",    "scguardgolem.manual.config.help"),
            new Topic(Items.NAME_TAG,              "scguardgolem.manual.allowdeny.title", "scguardgolem.manual.allowdeny.help"),
            new Topic(Items.CHEST,                 "scguardgolem.manual.loot.title",      "scguardgolem.manual.loot.help"),
            new Topic(Items.BELL,                  "scguardgolem.manual.patrol.title",    "scguardgolem.manual.patrol.help"),
            new Topic(SCGContent.EMP_GUN.get(),    "scguardgolem.manual.empgun.title",    "scguardgolem.manual.empgun.help"),
            new Topic(Items.LEAD,                  "scguardgolem.manual.taming.title",    "scguardgolem.manual.taming.help")
        );
    }

    /** Every page: a 6-field record on {@literal <=}1.21.1, 7-field (empty-recipe supplier) on 1.21.8+. */
    public static List<SCManualPage> buildPages() {
        List<SCManualPage> pages = new ArrayList<>();
        for (Topic t : topics()) {
            Component title = Component.translatable(t.titleKey());
            Component help = Component.translatable(t.helpKey());
            //? if >=1.21.8 {
            Supplier<Optional<List<RecipeDisplay>>> noRecipe = () -> Optional.empty();
            pages.add(new SCManualPage(t.icon(), PageGroup.NONE, title, help, "SCGuardGolem Team", false, noRecipe));
            //?} else
            /*pages.add(new SCManualPage(t.icon(), PageGroup.NONE, title, help, "SCGuardGolem Team", false));*/
        }
        return pages;
    }

    /** Add each page whose icon isn't already present (per-item dedup; safe to run repeatedly). */
    private static void addPagesIfAbsent() {
        for (SCManualPage page : buildPages()) {
            Item icon = page.item();
            if (SCManualItem.PAGES.stream().noneMatch(p -> p.item() == icon))
                SCManualItem.PAGES.add(page);
        }
    }

    // 1.21.8+ : add server-side at HIGH priority (before SC's NORMAL sync sender), deduped.
    //? if >=1.21.8 {
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        addPagesIfAbsent();
    }

    public static EventPriority syncPriority() {
        return EventPriority.HIGH;
    }
    //?} else {
    /*public static void onInterModProcess(InterModProcessEvent event) {
        event.enqueueWork(SCGManualPages::addPagesIfAbsent);
    }
    *///?}
}
