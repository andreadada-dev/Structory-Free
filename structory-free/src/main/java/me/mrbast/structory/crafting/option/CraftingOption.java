package me.mrbast.structory.crafting.option;

import me.mrbast.dadaconfig.logic.ConfigSection;
import me.mrbast.structory.Structory;
import me.mrbast.structory.crafting.Crafting;
import me.mrbast.structory.crafting.CraftingSettings;
import me.mrbast.structory.crafting.decoration.CraftingDecoration;
import me.mrbast.structory.crafting.layout.RecipeSlotLayout;
import me.mrbast.structory.crafting.listener.CraftingInteractionListener;
import me.mrbast.structory.crafting.recipe.Recipe;
import me.mrbast.structory.crafting.recipe.craftable.DirectDiscoveredRecipe;
import me.mrbast.structory.crafting.recipe.craftable.DiscoveredRecipe;
import me.mrbast.structory.crafting.recipe.craftable.GroupDiscoveredRecipe;
import me.mrbast.structory.enums.StructureSpacedKey;
import me.mrbast.structory.event.Listener;
import me.mrbast.structory.event.LoadStructureInstance;
import me.mrbast.structory.event.StructureEventHandler;
import me.mrbast.structory.interaction.InteractListener;
import me.mrbast.structory.option.HasInstanceData;
import me.mrbast.structory.option.InteractableOption;
import me.mrbast.structory.option.Option;
import me.mrbast.structory.structure.Structure;
import me.mrbast.structory.structure.StructureInstance;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CraftingOption implements Option, InteractableOption, HasInstanceData {

    private static final CraftingOption INSTANCE = new CraftingOption();

    public static CraftingOption getInstance() {
        return INSTANCE;
    }

    private final Map<StructureInstance, Crafting> craftingMap = new ConcurrentHashMap<>();
    private final Map<Structure, CraftingSettings> craftingDataMap = new ConcurrentHashMap<>();
    private final Map<StructureInstance, Set<NamespacedKey>> loadedEventDiscoveredRecipe = new ConcurrentHashMap<>();
    private final InteractListener interactListener = (instance, event) -> onInteract(event, instance);

    private CraftingOption() {
        Structory plugin = Structory.getPlugin(Structory.class);
        plugin.getServer().getPluginManager().registerEvents(new CraftingInteractionListener(this), plugin);
    }

    public void dropAllRecipeItems() {
        craftingMap.values().forEach(Crafting::dropAll);
    }

    /** Clears only configuration/instance runtime caches; Bukkit listeners stay registered once. */
    public void clearRuntimeState() {
        craftingMap.clear();
        craftingDataMap.clear();
        loadedEventDiscoveredRecipe.clear();
    }

    @Override
    public InteractListener getInteractListener() {
        return interactListener;
    }

    public Optional<Crafting> getCrafting(StructureInstance instance) {
        return Optional.ofNullable(craftingMap.get(instance));
    }

    public void onInteract(PlayerInteractEvent event, StructureInstance instance) {
        Crafting crafting = craftingMap.get(instance);
        if (crafting != null) crafting.craftEvent(event.getPlayer());
    }

    private final Listener listener = new Listener() {
        @StructureEventHandler
        public void onLoad(LoadStructureInstance event) {
            StructureInstance instance = event.getInstance();
            CraftingSettings settings = craftingDataMap.get(instance.getData().getStructure());
            if (settings == null || settings.getRecipeSlotLayout() == null) return;

            Crafting crafting = new Crafting(instance);
            RecipeSlotLayout layout = settings.getRecipeSlotLayout();
            layout.generate(instance, crafting);
            craftingMap.put(instance, crafting);

            settings.getDiscoveredRecipes().forEach(discovered ->
                    discovered.getRecipes().forEach(crafting::discoverRecipe));

            Set<NamespacedKey> loaded = loadedEventDiscoveredRecipe.remove(instance);
            if (loaded != null) {
                loaded.forEach(key -> Optional.ofNullable(me.mrbast.structory.manager.RecipeManager.getInstance().getRecipe(key))
                        .ifPresent(crafting::discoverRecipe));
            }
        }
    };

    @Override
    public void read(Structure structure, ConfigSection section) {
        CraftingSettings data = new CraftingSettings();

        section.getSection("insert").flatMap(s -> s.read(CraftingDecoration.class)).ifPresent(data::setInsert);
        section.getSection("result").flatMap(s -> s.read(CraftingDecoration.class)).ifPresent(data::setCraft);
        section.getSection("place").flatMap(s -> s.read(CraftingDecoration.class)).ifPresent(data::setPlace);
        section.getSection("take").flatMap(s -> s.read(CraftingDecoration.class)).ifPresent(data::setTake);
        section.getSection("consume").flatMap(s -> s.read(CraftingDecoration.class)).ifPresent(data::setConsume);
        section.getSection("recipe-slots").flatMap(s -> s.read(RecipeSlotLayout.class)).ifPresent(data::setRecipeSlotLayout);

        List<DiscoveredRecipe> discovered = new ArrayList<>();
        if (section.contains("recipe-group")) {
            section.getStringList("recipe-group").forEach(key -> discovered.add(new GroupDiscoveredRecipe(key)));
        }
        if (section.contains("recipes")) {
            section.getStringList("recipes").forEach(key -> discovered.add(new DirectDiscoveredRecipe(key)));
        }
        if (discovered.isEmpty()) discovered.add(new GroupDiscoveredRecipe("DEFAULT"));

        data.setDiscoveredRecipes(discovered);
        structure.getInteraction().subscribeListener(this);
        craftingDataMap.put(structure, data);
    }

    @Override
    public void write(ConfigSection configSection) {
    }

    @Override
    public Option getOption() {
        return this;
    }

    @Override
    public StructureSpacedKey getKey() {
        return StructureSpacedKey.OPTION_CRAFTING;
    }

    @Override
    public void init(Structure structure) {
    }

    @Override
    public void init() {
    }

    public Optional<CraftingSettings> getCraftingData(Structure structure) {
        return Optional.ofNullable(craftingDataMap.get(structure));
    }

    public Map<Structure, CraftingSettings> getCraftingDataMap() {
        return craftingDataMap;
    }

    @Override
    public void load(ConfigSection section, String path, StructureInstance instance) {
        section.getSection("crafting").ifPresent(craftingSection -> {
            String recipes = craftingSection.getString("discoveredRecipes");
            if (recipes == null || recipes.isEmpty()) return;

            Set<NamespacedKey> keys = ConcurrentHashMap.newKeySet();
            loadedEventDiscoveredRecipe.put(instance, keys);
            Arrays.stream(recipes.split("-"))
                    .map(NamespacedKey::fromString)
                    .filter(java.util.Objects::nonNull)
                    .forEach(keys::add);
        });
    }

    @Override
    public void save(ConfigSection section, String path, StructureInstance instance) {
        CraftingSettings settings = craftingDataMap.get(instance.getData().getStructure());
        if (settings == null) return;

        if (!section.contains("crafting")) section.createSection("crafting");
        section.getSection("crafting").ifPresent(craftingSection ->
                getCrafting(instance).ifPresent(crafting -> {
                    List<Recipe> defaults = new ArrayList<>();
                    settings.getDiscoveredRecipes().forEach(discovered -> defaults.addAll(discovered.getRecipes()));

                    StringBuilder saver = new StringBuilder();
                    crafting.getDiscoveredRecipes().forEach(recipe -> {
                        if (!defaults.contains(recipe)) saver.append(recipe.getKey()).append('-');
                    });
                    if (saver.length() == 0) return;
                    saver.setLength(saver.length() - 1);
                    craftingSection.write(String.class, "discoveredRecipes", saver.toString());
                }));
    }
}
