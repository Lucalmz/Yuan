package com.bear27570.yuan.BotFactory;

import com.bear27570.yuan.BotFactory.Structure.StructureLink;

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
