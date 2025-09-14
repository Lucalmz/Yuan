package com.bear27570.yuan.BotFactory.Model;

import com.bear27570.yuan.BotFactory.Action;
import com.bear27570.yuan.BotFactory.Structure.StructureLink;
/**
 * 结构组和动作的共同封装类，用于safety check
 */
public class StructureActionPair{
    private StructureLink structureLink;
    private Action thisAct;
    public StructureActionPair(StructureLink structureLink,Action thisAct){
        this.structureLink = structureLink;
        this.thisAct = thisAct;
    }
    //SafetyCheck直接run
    public void run(){
        structureLink.act(thisAct);
    }
    //获取对应动作
    public Action getRelevantAction(){
        return thisAct;
    }
    //读取StructureLink
    public StructureLink getStructure(){
        return structureLink;
    }
}
