package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Structure.StructureLink;
/**
 * 结构组和动作的共同封装类，用于safety check
 */
public class StructureActionPair {
    private StructureLink structureLink;
    private Action thisAct;
    public StructureActionPair(StructureLink structureLink,Action thisAct){
        this.structureLink = structureLink;
        this.thisAct = thisAct;
    }
    public void run(){
        structureLink.act(thisAct);
    }
    public Action getRelevantAction(){
        return thisAct;
    }
    public StructureLink getStructure(){
        return structureLink;
    }
}
