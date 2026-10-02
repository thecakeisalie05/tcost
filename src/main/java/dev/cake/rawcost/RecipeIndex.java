package dev.cake.rawcost;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.oredict.OreDictionary;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.api.enums.GTValues;

public final class RecipeIndex {
    public final Planner planner=new Planner();
    public final Set<String> machines=new TreeSet<>();
    public final Map<String,String> machineNames=new TreeMap<>();
    public int skippedChance, skippedUnknown;
    public static String itemKey(ItemStack s) {
        return "item:"+net.minecraft.item.Item.itemRegistry.getNameForObject(s.getItem())+":"+s.getItemDamage()
            +(s.hasTagCompound()?":"+s.getTagCompound().toString():"");
    }
    public static String fluidKey(FluidStack s) {
        return "fluid:"+s.getFluid().getName()+(s.tag==null?"":":"+s.tag.toString());
    }
    private Planner.Amount item(ItemStack s) {
        String k=itemKey(s); planner.names.put(k,s.getDisplayName()); return new Planner.Amount(k,s.stackSize);
    }
    private Planner.Amount fluid(FluidStack s) {
        String k=fluidKey(s); planner.names.put(k,s.getLocalizedName()+" (mB)"); return new Planner.Amount(k,s.amount);
    }
    public void build() {
        machines.add("crafting"); machines.add("furnace");
        machineNames.put("crafting","Crafting table"); machineNames.put("furnace","Vanilla furnace");
        List<RecipeMap<?>> maps=new ArrayList<>(RecipeMap.ALL_RECIPE_MAPS.values());
        maps.sort(Comparator.comparing(m->m.unlocalizedName));
        for(RecipeMap<?> map:maps) {
            String machine=map.unlocalizedName; machines.add(machine);
            machineNames.put(machine,net.minecraft.util.StatCollector.translateToLocal(machine));
            for(GTRecipe r:map.getAllRecipes()) {
                if(!r.mFakeRecipe) {
                    if(r.mOutputs!=null)for(ItemStack s:r.mOutputs)if(s!=null)planner.knownProduced.add(item(s).key);
                    if(r.mFluidOutputs!=null)for(FluidStack s:r.mFluidOutputs)if(s!=null)planner.knownProduced.add(fluid(s).key);
                }
                if(!r.mEnabled||r.mHidden||r.mFakeRecipe||r.mEUt<0) continue;
                List<Planner.Amount> in=new ArrayList<>(),out=new ArrayList<>();
                boolean chance=false;
                if(r.mInputs!=null) for(int i=0;i<r.mInputs.length;i++) {
                    int c=r.getInputChance(i);
                    ItemStack s=r.mInputs[i];
                    if(s!=null&&s.stackSize>0&&c>0) {
                        if(r instanceof GTRecipe.GTRecipe_WithAlt) {
                            ItemStack[][] alts=((GTRecipe.GTRecipe_WithAlt)r).mOreDictAlt;
                            if(alts!=null&&i<alts.length&&alts[i]!=null&&alts[i].length>0) {
                                List<Planner.Amount> choices=new ArrayList<>();
                                for(ItemStack alt:alts[i])if(alt!=null&&alt.stackSize>0)choices.add(item(alt));
                                in.add(choices.isEmpty()?item(s):alternativeAmounts(choices));
                            } else in.add(item(s));
                        } else in.add(item(s));
                    }
                    if(c!=0&&c!=10000) chance=true;
                }
                if(r.mFluidInputs!=null) for(int i=0;i<r.mFluidInputs.length;i++) {
                    int c=r.mFluidInputChances!=null&&i<r.mFluidInputChances.length?r.mFluidInputChances[i]:10000;
                    FluidStack s=r.mFluidInputs[i]; if(s!=null&&s.amount>0&&c>0) in.add(fluid(s));
                    if(c!=0&&c!=10000) chance=true;
                }
                if(r.mOutputs!=null) for(int i=0;i<r.mOutputs.length;i++) {
                    int c=r.getOutputChance(i);
                    ItemStack s=r.mOutputs[i]; if(s!=null&&s.stackSize>0&&c>0) out.add(item(s));
                    if(c!=0&&c!=10000) chance=true;
                }
                if(r.mFluidOutputs!=null) for(int i=0;i<r.mFluidOutputs.length;i++) {
                    int c=r.mFluidOutputChances!=null&&i<r.mFluidOutputChances.length?r.mFluidOutputChances[i]:10000;
                    FluidStack s=r.mFluidOutputs[i]; if(s!=null&&s.amount>0&&c>0) out.add(fluid(s));
                    if(c!=0&&c!=10000) chance=true;
                }
                if(chance) { skippedChance++; continue; }
                int tier=0; double voltage=(double)r.mEUt/Math.max(1,map.getAmperage());
                while(tier<GTValues.V.length-1&&voltage>GTValues.V[tier]) tier++;
                boolean special=r.mSpecialValue!=0||r.mSpecialItems!=null;
                // Metadata can impose coil/temperature/cleanroom/other requirements not represented by voltage.
                if(!r.getMetadataStorage().getEntries().isEmpty()) special=true;
                add(machine,in,out,r.mDuration,r.mEUt*(double)r.mDuration,tier,special);
            }
        }
        machines.add("assembly_line");machineNames.put("assembly_line","Assembly line");
        for(GTRecipe.RecipeAssemblyLine r:GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes) {
            if(r.mOutput==null)continue;
            List<Planner.Amount> in=new ArrayList<>();
            if(r.mInputs!=null) for(int i=0;i<r.mInputs.length;i++) {
                ItemStack s=r.mInputs[i];if(s==null||s.stackSize<=0)continue;
                if(r.mOreDictAlt!=null&&i<r.mOreDictAlt.length&&r.mOreDictAlt[i]!=null&&r.mOreDictAlt[i].length>0) {
                    List<Planner.Amount> choices=new ArrayList<>();
                    for(ItemStack alt:r.mOreDictAlt[i])if(alt!=null&&alt.stackSize>0)choices.add(item(alt));
                    in.add(choices.isEmpty()?item(s):alternativeAmounts(choices));
                } else in.add(item(s));
            }
            if(r.mFluidInputs!=null)for(FluidStack s:r.mFluidInputs)if(s!=null&&s.amount>0)in.add(fluid(s));
            int tier=0;while(tier<GTValues.V.length-1&&r.mEUt>GTValues.V[tier])tier++;
            // Research is a prerequisite, not recurring raw consumption; explicitly declare it available.
            add("assembly_line",in,Collections.singletonList(item(r.mOutput)),r.mDuration,r.mEUt*(double)r.mDuration,tier,true);
        }
        for(Object object:CraftingManager.getInstance().getRecipeList()) {
            IRecipe recipe=(IRecipe)object; ItemStack output=recipe.getRecipeOutput();
            if(output==null) continue;
            planner.knownProduced.add(item(output).key);
            Object[] inputs;
            if(recipe instanceof ShapedRecipes) inputs=((ShapedRecipes)recipe).recipeItems;
            else if(recipe instanceof ShapelessRecipes) inputs=((ShapelessRecipes)recipe).recipeItems.toArray();
            else if(recipe instanceof ShapedOreRecipe) inputs=((ShapedOreRecipe)recipe).getInput();
            else if(recipe instanceof ShapelessOreRecipe) inputs=((ShapelessOreRecipe)recipe).getInput().toArray();
            else { skippedUnknown++; continue; }
            List<Planner.Amount> in=new ArrayList<>(); boolean valid=true, special=false;
            for(Object ingredient:inputs) {
                if(ingredient==null) continue;
                if(ingredient instanceof ItemStack) {
                    ItemStack stack=((ItemStack)ingredient).copy(); stack.stackSize=1;
                    if(stack.getItem().hasContainerItem(stack)) special=true;
                    Planner.Amount a=choice(Collections.singletonList(stack));
                    if(a==null) valid=false; else in.add(a);
                } else if(ingredient instanceof List) {
                    List<ItemStack> choices=new ArrayList<>();
                    for(Object v:(List<?>)ingredient) if(v instanceof ItemStack) choices.add((ItemStack)v);
                    Planner.Amount a=choice(choices); if(a==null) valid=false; else in.add(a);
                } else valid=false;
            }
            if(valid) add("crafting",in,Collections.singletonList(item(output)),0,0,0,special);
            else skippedUnknown++;
        }
        for(Object object:FurnaceRecipes.smelting().getSmeltingList().entrySet()) {
            Map.Entry<?,?> entry=(Map.Entry<?,?>)object;
            ItemStack input=((ItemStack)entry.getKey()).copy(); input.stackSize=1;
            Planner.Amount a=choice(Collections.singletonList(input));
            if(a!=null) add("furnace",Collections.singletonList(a),Collections.singletonList(item((ItemStack)entry.getValue())),200,0,0,false);
        }
    }
    private Planner.Amount alternativeAmounts(List<Planner.Amount> choices) {
        if(choices.isEmpty())throw new IllegalArgumentException("Empty GT ingredient alternatives");
        if(choices.size()==1)return choices.get(0);
        List<String> signature=new ArrayList<>();for(Planner.Amount a:choices)signature.add(a.key+"="+a.count);Collections.sort(signature);
        String alias="choice:"+digest(signature.toString());planner.names.put(alias,"Ingredient alternatives");
        for(Planner.Amount a:choices)add("ingredient",Collections.singletonList(a),Collections.singletonList(new Planner.Amount(alias,1)),0,0,0,false);
        return new Planner.Amount(alias,1);
    }
    private Planner.Amount choice(List<ItemStack> stacks) {
        List<ItemStack> expanded=new ArrayList<>();
        for(ItemStack s:stacks) {
            if(s.getItemDamage()==OreDictionary.WILDCARD_VALUE) {
                List<ItemStack> sub=new ArrayList<>();
                s.getItem().getSubItems(s.getItem(),null,sub);
                expanded.addAll(sub);
            } else expanded.add(s);
        }
        if(expanded.isEmpty()) return null;
        if(expanded.size()==1) {
            ItemStack single=expanded.get(0).copy();single.stackSize=1;return item(single);
        }
        List<String> keys=new ArrayList<>();
        for(ItemStack s:expanded) keys.add(itemKey(s)); Collections.sort(keys);
        String alias="choice:"+digest(keys.toString()); planner.names.put(alias,"Ingredient alternatives");
        for(ItemStack s:expanded) {
            ItemStack single=s.copy(); single.stackSize=1;
            add("ingredient",Collections.singletonList(item(single)),Collections.singletonList(new Planner.Amount(alias,1)),0,0,0,false);
        }
        return new Planner.Amount(alias,1);
    }
    private void add(String machine,List<Planner.Amount> in,List<Planner.Amount> out,double ticks,double eu,int tier,boolean special) {
        if(out.isEmpty()) return;
        StringBuilder signature=new StringBuilder(machine).append('|').append(ticks).append('|').append(eu).append('|').append(tier).append('|').append(special);
        for(Planner.Amount a:in) signature.append("|I:").append(a.key).append('=').append(a.count);
        for(Planner.Amount a:out) signature.append("|O:").append(a.key).append('=').append(a.count);
        String id=machine+":"+digest(signature.toString());
        planner.register(new Planner.Route(id,machine,in,out,ticks,eu,tier,special));
    }
    private static String digest(String s) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder(); for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255)); return b.toString();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
}
