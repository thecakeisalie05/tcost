package dev.cake.rawcost;

import java.util.Collections;

/** Dependency-free executable regression checks: ./scripts/test-core.sh */
public final class PlannerChecks {
    private static Planner.Amount a(String key,double n){return new Planner.Amount(key,n);}
    private static Planner.Route route(String id,String machine,String in,double count,String out,double yield,double ticks,double eu,int tier){
        return new Planner.Route(id,machine,Collections.singletonList(a(in,count)),Collections.singletonList(a(out,yield)),ticks,eu,tier,false);
    }
    private static void check(boolean ok,String what){if(!ok)throw new AssertionError(what);}
    public static void main(String[] args) {
        Planner p=new Planner();Planner.Profile s=new Planner.Profile();s.machines.put("assembler",2);
        p.register(route("batch","assembler","iron",3,"part",2,100,200,1));
        Planner.Result r=p.calculate("part",3,s);
        check(r.raw.get("iron")==6,"whole recipe batches");check(r.surplus.get("part")==1,"surplus accounting");
        check(r.eu==400&&r.ticks==200,"EU/time across batches");
        s.machineCounts.put("assembler",2);check(p.calculate("part",3,s).ticks==100,"machine count affects time, not EU");
        s.machines.put("assembler",0);check(!p.calculate("part",1,s).unresolved.isEmpty(),"tier filtering");
        s.machines.put("assembler",2);s.raw.add("part");check(p.calculate("part",3,s).raw.get("part")==3,"explicit terminal");s.raw.clear();
        p.register(route("lean","assembler","copper",1,"part",1,400,50,1));
        s.objective=Planner.Objective.ENERGY;check(p.calculate("part",1,s).raw.containsKey("copper"),"energy objective");
        s.objective=Planner.Objective.TIME;check(p.calculate("part",1,s).raw.containsKey("iron"),"time objective");
        s.objective=Planner.Objective.MATERIALS;s.prices.put("copper",100.0);check(p.calculate("part",1,s).raw.containsKey("iron"),"material weights");
        s.preferred.put("part","lean");check(p.calculate("part",1,s).raw.containsKey("copper"),"pinned recipe");
        s.preferred.put("part","deleted");check(!p.calculate("part",1,s).unresolved.isEmpty(),"stale pin is unresolved");s.preferred.clear();
        Planner loop=new Planner();loop.register(route("ab","assembler","b",1,"a",1,1,1,0));loop.register(route("ba","assembler","a",1,"b",1,1,1,0));
        check(!loop.calculate("a",1,s).unresolved.isEmpty(),"cycle detection");
        loop.register(route("source","assembler","ore",2,"b",1,2,2,0));
        check(loop.calculate("a",1,s).unresolved.isEmpty(),"complete alternative beats cycle");
        s.maxDepth=1;check(loop.calculate("a",1,s).limited,"depth bound");s.maxDepth=32;
        Planner special=new Planner();special.register(new Planner.Route("cleanroom","assembler",Collections.singletonList(a("ore",1)),Collections.singletonList(a("chip",1)),10,10,1,true));
        check(!special.calculate("chip",1,s).unresolved.isEmpty(),"special requirements unavailable by default");
        s.allowedSpecial.add("cleanroom");check(special.calculate("chip",1,s).unresolved.isEmpty(),"special explicit approval");
        special.knownProduced.add("unsupported");check(!special.calculate("unsupported",1,s).unresolved.isEmpty(),"unsupported output is not free raw material");
        boolean rejected=false;try{p.calculate("part",Double.NaN,s);}catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"invalid quantity rejected");
        Planner choices=new Planner();choices.register(route("choice","ingredient","ore",1,"choice:test",1,0,0,0));
        Planner.Profile noCrafting=new Planner.Profile();Planner.Result choice=choices.calculate("choice:test",1,noCrafting);
        check(choice.unresolved.isEmpty()&&choice.operations==0,"ingredient alternatives do not require crafting availability or count as operations");
        System.out.println("Planner checks passed (batching, surplus, tier, quantity, parallelism, weights, objectives, pins, cycles, bounds, special requirements, unsupported outputs).");
    }
}
