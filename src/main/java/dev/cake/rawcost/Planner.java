package dev.cake.rawcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Minecraft-independent, bounded route evaluator. Quantities are items or millibuckets. */
public final class Planner {
    public enum Objective { MATERIALS, ENERGY, TIME, BALANCED, STEPS }
    public static final class Amount {
        public final String key;
        public final double count;
        public Amount(String key, double count) { this.key = key; this.count = count; }
    }
    public static final class Route {
        public final String id, machine;
        public final List<Amount> inputs, outputs;
        public final double ticks, eu;
        public final int tier;
        public final boolean special;
        public Route(String id, String machine, List<Amount> inputs, List<Amount> outputs,
                     double ticks, double eu, int tier, boolean special) {
            this.id=id; this.machine=machine; this.inputs=inputs; this.outputs=outputs;
            this.ticks=ticks; this.eu=eu; this.tier=tier; this.special=special;
        }
        public double output(String key) {
            double n=0; for (Amount a:outputs) if (a.key.equals(key)) n+=a.count; return n;
        }
    }
    public static final class Profile {
        public final Map<String,Integer> machines=new TreeMap<>();
        public final Map<String,Integer> machineCounts=new TreeMap<>();
        public final Set<String> raw=new TreeSet<>(), allowedSpecial=new TreeSet<>();
        public final Map<String,String> preferred=new TreeMap<>();
        public final Map<String,Double> prices=new TreeMap<>();
        public Objective objective=Objective.MATERIALS;
        public double materialWeight=1, energyWeight=0.000001, timeWeight=0.001;
        public int maxDepth=32, maxVisits=12000;
        public boolean available(Route r) {
            if(r.machine.equals("ingredient")) return true;
            Integer tier=machines.get(r.machine);
            return tier!=null && tier>=r.tier && (!r.special || allowedSpecial.contains(r.id));
        }
    }
    public static final class Result {
        public final Map<String,Double> raw=new TreeMap<>(), unresolved=new TreeMap<>(), surplus=new TreeMap<>();
        public final List<String> steps=new ArrayList<>();
        public double ticks, eu, operations;
        public boolean limited;
        public void merge(Result b) {
            addAll(raw,b.raw); addAll(unresolved,b.unresolved); addAll(surplus,b.surplus);
            steps.addAll(b.steps); ticks+=b.ticks; eu+=b.eu; operations+=b.operations; limited|=b.limited;
        }
    }
    private final Map<String,List<Route>> index=new TreeMap<>();
    public final Map<String,Route> routes=new TreeMap<>();
    public final Map<String,String> names=new TreeMap<>();
    public final Set<String> knownProduced=new HashSet<>();
    private int visits;
    public void register(Route r) {
        if(routes.containsKey(r.id)) return;
        routes.put(r.id,r);
        for(Amount a:r.outputs) if(a.count>0) {knownProduced.add(a.key);index.computeIfAbsent(a.key,k->new ArrayList<>()).add(r);}
    }
    public List<Route> alternatives(String key) {
        List<Route> r=index.get(key); return r==null?Collections.emptyList():r;
    }
    public synchronized Result calculate(String key,double quantity,Profile p) {
        if(!Double.isFinite(quantity)||quantity<=0) throw new IllegalArgumentException("Positive quantity required");
        visits=0; return expand(key,quantity,p,new HashSet<>(),0);
    }
    private Result expand(String key,double count,Profile p,Set<String> path,int depth) {
        Result leaf=new Result();
        if(p.raw.contains(key)) {add(leaf.raw,key,count);return leaf;}
        if(!index.containsKey(key)) {add(knownProduced.contains(key)?leaf.unresolved:leaf.raw,key,count);return leaf;}
        if(depth>=p.maxDepth||++visits>p.maxVisits||path.contains(key)) {
            add(leaf.unresolved,key,count); leaf.limited=true; return leaf;
        }
        path.add(key);
        Result best=null;
        List<Route> candidates=new ArrayList<>(alternatives(key));
        candidates.sort(Comparator.comparing(r->r.id));
        for(Route r:candidates) {
            if(!p.available(r)) continue;
            String pin=p.preferred.get(key);
            if(pin!=null&&!pin.equals(r.id)) continue;
            double batches=Math.ceil(count/r.output(key)-1e-10);
            if(!Double.isFinite(batches)||batches<=0) continue;
            Result result=new Result();
            int machines=Math.max(1,p.machineCounts.getOrDefault(r.machine,1));
            result.ticks=Math.ceil(batches/machines)*r.ticks; result.eu=batches*r.eu; result.operations=r.machine.equals("ingredient")?0:batches;
            for(Amount input:r.inputs) if(input.count>0) result.merge(expand(input.key,input.count*batches,p,path,depth+1));
            double extra=batches*r.output(key)-count;
            if(extra>1e-8) add(result.surplus,key,extra);
            for(Amount out:r.outputs) if(!out.key.equals(key)) add(result.surplus,out.key,out.count*batches);
            result.steps.add(r.id+" x "+(long)batches);
            if(best==null||better(result,best,p)) best=result;
            if(visits>p.maxVisits) break;
        }
        path.remove(key);
        if(best!=null) return best;
        add(leaf.unresolved,key,count); leaf.limited=visits>p.maxVisits; return leaf;
    }
    private boolean better(Result a,Result b,Profile p) {
        // Incomplete routes never win against a complete route merely by looking cheaper.
        if(a.unresolved.isEmpty()!=b.unresolved.isEmpty()) return a.unresolved.isEmpty();
        if(a.limited!=b.limited) return !a.limited;
        return score(a,p)<score(b,p);
    }
    public static double materialCost(Result r,Profile p) {
        double n=0; for(Map.Entry<String,Double> e:r.raw.entrySet()) {
            double price=p.prices.getOrDefault(e.getKey(),e.getKey().startsWith("fluid:")?0.001:1.0);
            n+=e.getValue()*price;
        } return n;
    }
    public static double score(Result r,Profile p) {
        switch(p.objective) {
            case ENERGY:return r.eu;
            case TIME:return r.ticks;
            case STEPS:return r.operations;
            case BALANCED:return materialCost(r,p)*p.materialWeight+r.eu*p.energyWeight+r.ticks*p.timeWeight;
            default:return materialCost(r,p);
        }
    }
    private static void addAll(Map<String,Double> a,Map<String,Double> b) { for(Map.Entry<String,Double> e:b.entrySet()) add(a,e.getKey(),e.getValue()); }
    private static void add(Map<String,Double> map,String key,double n) { map.put(key,map.getOrDefault(key,0.0)+n); }
}
