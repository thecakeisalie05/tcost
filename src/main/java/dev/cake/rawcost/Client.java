package dev.cake.rawcost;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

public final class Client extends Common implements IContainerTooltipHandler,IContainerInputHandler {
    public static Client instance;
    public RecipeIndex index;
    public Settings settings;
    private File file;
    public String status="Loading recipe index...";
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"TCost planner");t.setDaemon(true);return t;});
    private final Map<String,Future<Planner.Result>> cache=new LinkedHashMap<>();
    private final KeyBinding menu=new KeyBinding("TCost: options / hovered item",Keyboard.KEY_F8,"TCost");
    public void preInit(FMLPreInitializationEvent e) {
        instance=this; file=new File(e.getModConfigurationDirectory(),"tcost.json"); settings=Settings.load(file);
        if(!file.isFile()) {
            settings.profile.machines.put("crafting",0); settings.profile.machines.put("furnace",0);
        }
        ClientRegistry.registerKeyBinding(menu);
        FMLCommonHandler.instance().bus().register(this);
        GuiContainerManager.addTooltipHandler(this); GuiContainerManager.addInputHandler(this);
    }
    public void complete() {
        try {
            RecipeIndex built=new RecipeIndex(); built.build(); index=built;
            status=built.planner.routes.size()+" routes indexed; "+built.skippedChance+" chance and "+built.skippedUnknown+" unsupported recipes skipped";
            TCost.LOG.info(status);
        } catch(Exception e) { status="Recipe index failed; see latest.log"; TCost.LOG.error(status,e); }
    }
    public void changed() {
        for(Future<?> f:cache.values()) f.cancel(false);
        cache.clear();
        try { settings.save(file); } catch(Exception e) { status="Settings could not be saved; see latest.log"; TCost.LOG.error(status,e); }
    }
    public Planner.Result result(String key) {
        if(index==null) return null;
        String cacheKey=key+"/"+settings.quantity;
        Future<Planner.Result> f=cache.get(cacheKey);
        if(f==null) {
            // Worker only sees the detached immutable snapshot and plain recipe data; no Minecraft calls.
            Planner.Profile p=Settings.JSON.fromJson(Settings.JSON.toJson(settings.profile),Planner.Profile.class);
            int q=settings.quantity;
            f=worker.submit(()->index.planner.calculate(key,q,p)); cache.put(cacheKey,f);
            if(cache.size()>64) { Iterator<Future<Planner.Result>> it=cache.values().iterator();it.next().cancel(false);it.remove(); }
        }
        if(!f.isDone()||f.isCancelled()) return null;
        try { return f.get(); } catch(Exception e) { TCost.LOG.error("Route evaluation failed",e);return null; }
    }
    public List<String> lines(String key,Planner.Result r,boolean full) {
        List<String> lines=new ArrayList<>();
        lines.add("Raw costs for "+settings.quantity+" x "+name(key));
        lines.add("Rule: "+settings.profile.objective+"; bounded route estimate");
        if(r==null) {lines.add(index==null?status:"Calculating...");return lines;}
        lines.add("EU: "+number(r.eu)+" | Time: "+number(r.ticks/20)+" s (base-speed estimate)");
        lines.add("Weighted material cost: "+number(Planner.materialCost(r,settings.profile)));
        for(Map.Entry<String,Double> e:r.raw.entrySet()) lines.add(number(e.getValue())+" x "+name(e.getKey()));
        if(!r.unresolved.isEmpty()) {
            lines.add("INCOMPLETE: unavailable recipes or a cycle");
            for(Map.Entry<String,Double> e:r.unresolved.entrySet()) lines.add("Missing: "+number(e.getValue())+" x "+name(e.getKey()));
        }
        if(r.limited) lines.add("Search limit reached; increase bounds in settings");
        if(full) {
            lines.add("Surplus / unused byproducts:");
            for(Map.Entry<String,Double> e:r.surplus.entrySet()) lines.add(number(e.getValue())+" x "+name(e.getKey()));
            lines.add("Selected processing route:");
            for(String step:r.steps) {
                String id=step.substring(0,step.lastIndexOf(" x "));
                Planner.Route route=index.planner.routes.get(id);
                if(route!=null&&!route.outputs.get(0).key.startsWith("choice:"))
                    lines.add(machineName(route.machine)+": "+name(route.outputs.get(0).key)+step.substring(step.lastIndexOf(" x ")));
            }
        }
        return lines;
    }
    public String name(String key) {return index==null?key:index.planner.names.getOrDefault(key,key);}
    public String machineName(String key) {return index==null?key:index.machineNames.getOrDefault(key,key);}
    public static String number(double n) {return String.format(Locale.ROOT,n==Math.rint(n)?"%,.0f":"%,.3f",n);}
    public List<String> handleItemTooltip(GuiContainer gui,ItemStack stack,int x,int y,List<String> tips) {
        if(!settings.hoverEnabled||index==null||!ItemPanels.itemPanel.contains(x,y)) return tips;
        Planner.Result r=result(RecipeIndex.itemKey(stack));
        List<String> costs=lines(RecipeIndex.itemKey(stack),r,false);
        tips.add("\u00a76TCost (F8: options, Ctrl+F8: hover toggle)");
        int max=Math.min(costs.size(),settings.hoverLines);
        for(int i=0;i<max;i++) tips.add("\u00a77"+costs.get(i));
        if(costs.size()>max) tips.add("\u00a77... F8 for full report");
        if(r!=null&&!r.unresolved.isEmpty()) tips.add("\u00a7cIncomplete estimate");
        return tips;
    }
    private void open(ItemStack stack) {
        if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)||Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
            settings.hoverEnabled=!settings.hoverEnabled;changed();
        } else Minecraft.getMinecraft().displayGuiScreen(new OptionsGui(Minecraft.getMinecraft().currentScreen,
            stack==null?null:RecipeIndex.itemKey(stack)));
    }
    @SubscribeEvent public void key(InputEvent.KeyInputEvent e) {
        if(Minecraft.getMinecraft().currentScreen==null&&menu.isPressed()) open(null);
    }
    public boolean lastKeyTyped(GuiContainer g,char c,int key) {if(key==menu.getKeyCode()){open(GuiContainerManager.getStackMouseOver(g));return true;}return false;}
    public boolean keyTyped(GuiContainer g,char c,int k){return false;}
    public void onKeyTyped(GuiContainer g,char c,int k){}
    public boolean mouseClicked(GuiContainer g,int x,int y,int b){return false;}
    public void onMouseClicked(GuiContainer g,int x,int y,int b){}
    public void onMouseUp(GuiContainer g,int x,int y,int b){}
    public boolean mouseScrolled(GuiContainer g,int x,int y,int s){return false;}
    public void onMouseScrolled(GuiContainer g,int x,int y,int s){}
    public void onMouseDragged(GuiContainer g,int x,int y,int b,long t){}
}
