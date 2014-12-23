package com.pinoo.core.dao;

import java.util.List;

import com.pinoo.beans.FieldInfo;
import com.pinoo.common.annotation.model.IdentityType;
import com.pinoo.common.annotation.model.ModelInfo;

public interface IEntityInfo {

    public FieldInfo getPrimaryKey();

    public FieldInfo getCtime();

    public FieldInfo getUtime();

    public String getSeqTableName();

    public String getTableName();

    public IdentityType getIdentityType();

    public Class getEntityClass();

    public List<FieldInfo> getFields();

    public ModelInfo getModelInfo();
}
