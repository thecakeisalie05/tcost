package dev.cake.rawcost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import gregtech.api.enums.GTValues;

/** Separate screen: never changes NEI's recipe panel or an inventory container. */
public final class OptionsGui extends GuiScreen {
    private final GuiScreen parent;
    private final Client client=Client.instance;
    private String target,selected;
    private int tab=0,offset=0,left,top,w,h,rows;
    private String filter="",message="";
    private GuiTextField search,quantity,material,energy,time,price;
    private final List<String> entries=new ArrayList<>();
    private final List<GuiTextField> fields=new ArrayList<>();
    public OptionsGui(GuiScreen parent,String target) {this.parent=parent;this.target=target;this.selected=target;}
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        w=Math.min(600,width-16);h=Math.min(410,height-16);left=(width-w)/2;top=(height-h)/2;
        rows=Math.max(1,(h-132)/24);buttonList.clear();fields.clear();
        String[] tabs={"Machines","Rules","Materials","Recipes","Report"};
        for(int i=0;i<5;i++) add(i,left+6+i*(w-12)/5,top+25,(w-12)/5-2,20,tabs[i]);
        add(9,left+w-68,top+h-26,60,20,"Done");
        if(tab!=1) {
            search=new GuiTextField(fontRendererObj,left+8,top+52,w-114,18);
            search.setMaxStringLength(100);search.setText(filter);fields.add(search);
            add(10,left+w-100,top+51,44,20,"<");add(11,left+w-54,top+51,44,20,">");
        }
        rebuildEntries();
        if(tab==0) {
            for(int i=0;i<rows&&offset+i<entries.size();i++) {
                String key=entries.get(offset+i);int tier=client.settings.profile.machines.getOrDefault(key,-1);
                add(1000+i,left+w-150,top+78+i*24,80,20,tier<0?"Unavailable":tierName(tier));
                add(2000+i,left+w-68,top+78+i*24,58,20,"x"+client.settings.profile.machineCounts.getOrDefault(key,1));
            }
        } else if(tab==1) {
            add(20,left+8,top+54,w-16,20,"Hover raw costs: "+(client.settings.hoverEnabled?"ON":"OFF"));
            add(21,left+8,top+78,w-16,20,"Optimization: "+client.settings.profile.objective);
            quantity=field(left+130,top+108,100,Integer.toString(client.settings.quantity));
            material=field(left+130,top+128,100,Double.toString(client.settings.profile.materialWeight));
            energy=field(left+130,top+148,100,Double.toString(client.settings.profile.energyWeight));
            time=field(left+130,top+168,100,Double.toString(client.settings.profile.timeWeight));
            add(22,left+8,top+190,100,20,"Apply values");
        } else if(tab==2) {
            for(int i=0;i<rows&&offset+i<entries.size();i++) add(1000+i,left+8,top+78+i*24,w-16,20,shorten(client.name(entries.get(offset+i)),w-32));
            if(selected!=null) {
                add(23,left+8,top+h-52,150,20,client.settings.profile.raw.contains(selected)?"Raw endpoint: YES":"Raw endpoint: NO");
                price=field(left+162,top+h-51,76,Double.toString(client.settings.profile.prices.getOrDefault(selected,selected.startsWith("fluid:")?0.001:1.0)));
                add(24,left+242,top+h-52,w-252,20,"Set cost");
            }
        } else if(tab==3) {
            for(int i=0;i<rows&&offset+i<entries.size();i++) {
                String id=entries.get(offset+i);Planner.Route r=client.index.planner.routes.get(id);
                String pinned=client.settings.profile.preferred.get(target);
                String label=(id.equals(pinned)?"* ":"")+client.machineName(r.machine)+" "+tierName(r.tier)+" / "+Client.number(r.ticks/20)+"s / "+Client.number(r.eu)+" EU";
                add(1000+i,left+8,top+78+i*24,w-94,20,shorten(label,w-110));
                if(r.special) add(2000+i,left+w-84,top+78+i*24,76,20,client.settings.profile.allowedSpecial.contains(id)?"Allow: YES":"Allow: NO");
            }
            if(target!=null) add(25,left+8,top+h-52,160,20,"Clear preferred recipe");
        }
    }
    private void rebuildEntries() {
        entries.clear();
        if(client.index==null) return;
        String f=filter.toLowerCase(Locale.ROOT);
        if(tab==0) for(String k:client.index.machines) {if(client.machineName(k).toLowerCase(Locale.ROOT).contains(f)||k.toLowerCase(Locale.ROOT).contains(f)) entries.add(k);}
        if(tab==2) for(String k:client.index.planner.names.keySet()) {if(!k.startsWith("choice:")&&(client.name(k).toLowerCase(Locale.ROOT).contains(f)||k.toLowerCase(Locale.ROOT).contains(f)))entries.add(k);}
        if(tab==3&&target!=null) for(Planner.Route r:client.index.planner.alternatives(target)) {if(client.machineName(r.machine).toLowerCase(Locale.ROOT).contains(f)||r.id.toLowerCase(Locale.ROOT).contains(f))entries.add(r.id);}
        Collections.sort(entries);
        if(tab!=4) offset=Math.max(0,Math.min(offset,Math.max(0,entries.size()-rows)));
    }
    private GuiTextField field(int x,int y,int width,String value) {
        GuiTextField f=new GuiTextField(fontRendererObj,x,y,width,18);f.setMaxStringLength(30);f.setText(value);fields.add(f);return f;
    }
    private void add(int id,int x,int y,int width,int height,String text) {buttonList.add(new GuiButton(id,x,y,width,height,text));}
    private String tierName(int t) {return t<GTValues.VN.length?GTValues.VN[t]:Integer.toString(t);}
    private String shorten(String text,int pixels) {return fontRendererObj.getStringWidth(text)<=pixels?text:fontRendererObj.trimStringToWidth(text,Math.max(0,pixels-10))+"...";}
    protected void actionPerformed(GuiButton b) {
        if(b.id<5) {tab=b.id;offset=0;filter="";message="";initGui();return;}
        if(b.id==9) {mc.displayGuiScreen(parent);return;}
        if(b.id==10||b.id==11) {offset=Math.max(0,offset+(b.id==10?-rows:rows));initGui();return;}
        if(b.id==20) client.settings.hoverEnabled=!client.settings.hoverEnabled;
        if(b.id==21) {Planner.Objective[] modes=Planner.Objective.values();client.settings.profile.objective=modes[(client.settings.profile.objective.ordinal()+1)%modes.length];}
        if(b.id==22) {
            try {
                int q=Integer.parseInt(quantity.getText());
                double m=positive(material.getText()),e=positive(energy.getText()),t=positive(time.getText());
                if(q<1||q>1000000) throw new IllegalArgumentException();
                client.settings.quantity=q;client.settings.profile.materialWeight=m;client.settings.profile.energyWeight=e;client.settings.profile.timeWeight=t;
                message="Values saved";
            } catch(Exception ex) {message="Enter nonnegative finite weights and quantity 1..1000000";return;}
        }
        if(b.id==23&&selected!=null) {
            if(!client.settings.profile.raw.remove(selected)) client.settings.profile.raw.add(selected);
        }
        if(b.id==24&&selected!=null) {
            try {client.settings.profile.prices.put(selected,positive(price.getText()));message="Material weight saved";}
            catch(Exception ex) {message="Enter a nonnegative finite material cost";return;}
        }
        if(b.id==25&&target!=null) client.settings.profile.preferred.remove(target);
        if(b.id>=1000) {
            int row=b.id%1000;if(offset+row>=entries.size())return;String key=entries.get(offset+row);
            if(tab==0) {
                if(b.id<2000) {int tier=client.settings.profile.machines.getOrDefault(key,-1)+1;if(tier>=GTValues.V.length)client.settings.profile.machines.remove(key);else client.settings.profile.machines.put(key,tier);}
                else {int n=client.settings.profile.machineCounts.getOrDefault(key,1);client.settings.profile.machineCounts.put(key,n>=64?1:n*2);}
            } else if(tab==2) {selected=key;target=key;}
            else if(tab==3) {
                if(b.id<2000) {client.settings.profile.preferred.put(target,key);message="Preferred recipe selected";}
                else {if(!client.settings.profile.allowedSpecial.remove(key))client.settings.profile.allowedSpecial.add(key);message="Special requirements are your explicit availability declaration";}
            }
        }
        client.changed();initGui();
    }
    private double positive(String text) {double n=Double.parseDouble(text);if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException();return n;}
    public void drawScreen(int x,int y,float partial) {
        drawDefaultBackground();drawRect(left,top,left+w,top+h,0xE0181E28);
        drawString(fontRendererObj,"TCost - "+(target==null?"Machine and route settings":shorten(client.name(target),w-75)),left+8,top+9,0xFFFFFF);
        if(tab==1) {
            String[] labels={"Target quantity","Material weight","Energy weight","Time weight"};
            for(int i=0;i<labels.length;i++)drawString(fontRendererObj,labels[i],left+8,top+113+i*20,0xCCCCCC);
            if(h>290) {
                drawString(fontRendererObj,"Time uses base speed; serial stages, parallel identical machines.",left+8,top+240,0xAAAAAA);
                drawString(fontRendererObj,"EU excludes overclocking and furnace fuel. No byproduct credits.",left+8,top+253,0xAAAAAA);
                drawString(fontRendererObj,"Recipe optimization is bounded and local, not a global guarantee.",left+8,top+266,0xAAAAAA);
            }
        } else if(tab==0) {
            for(int i=0;i<rows&&offset+i<entries.size();i++)drawString(fontRendererObj,shorten(client.machineName(entries.get(offset+i)),w-172),left+8,top+85+i*24,0xCCCCCC);
        } else if(tab==4&&target!=null) {
            List<String> lines=client.lines(target,client.result(target),true);
            if(!filter.isEmpty()) lines.removeIf(s->!s.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)));
            offset=Math.max(0,Math.min(offset,Math.max(0,lines.size()-rows)));
            for(int i=0;i<rows&&offset+i<lines.size();i++)drawString(fontRendererObj,shorten(lines.get(offset+i),w-20),left+8,top+85+i*24,0xCCCCCC);
        } else if((tab==3||tab==4)&&target==null) {
            drawString(fontRendererObj,"Hover an item in NEI and press F8, or choose Materials.",left+8,top+87,0xCCCCCC);
        }
        for(GuiTextField f:fields)f.drawTextBox();
        int statusX=tab==1&&h<250?118:8;
        drawString(fontRendererObj,shorten(message.isEmpty()?client.status:message,w-statusX-74),left+statusX,top+h-20,0xAABBDD);
        super.drawScreen(x,y,partial);
    }
    protected void keyTyped(char c,int k) {
        if(k==Keyboard.KEY_ESCAPE){mc.displayGuiScreen(parent);return;}
        for(GuiTextField f:fields) if(f.textboxKeyTyped(c,k)) {
            if(f==search) {filter=search.getText();offset=0;initGui();search.setFocused(true);}return;
        }
        super.keyTyped(c,k);
    }
    protected void mouseClicked(int x,int y,int button) {super.mouseClicked(x,y,button);for(GuiTextField f:fields)f.mouseClicked(x,y,button);}
    public void handleMouseInput() {super.handleMouseInput();int delta=Mouse.getEventDWheel();if(delta!=0&&tab!=1){offset=Math.max(0,offset+(delta<0?rows:-rows));initGui();}}
    public void updateScreen(){for(GuiTextField f:fields)f.updateCursorCounter();}
    public void onGuiClosed(){Keyboard.enableRepeatEvents(false);}
    public boolean doesGuiPauseGame(){return false;}
}
