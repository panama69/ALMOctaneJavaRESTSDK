/*
 * Copyright 2017 Hewlett-Packard Enterprise Development Company, L.P.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.adm.nga.sdk.model;


import com.hpe.adm.nga.sdk.model.*;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestModel {

    private static Set<FieldModel> set;
    private EntityModel model;
    private String expectedResult;
    private String gotResult;

    @BeforeClass
    public static void initializeOnCreate() {
        set = new HashSet<FieldModel>();
    }

    @Before
    public void beforeEachMethod() {
        set.clear();
    }

    @After
    public void afterEachMethod() {
        assertEquals(expectedResult, gotResult);
    }

    @Test
    public void testEntityModelWithBooleanField() {
        expectedResult = "{\"falseValue\":false,\"trueValue\":true}";
        set.add(new BooleanFieldModel("trueValue", true));
        set.add(new BooleanFieldModel("falseValue", false));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }

    }

    @Test
    public void testEntityModelWithStringField() {
        expectedResult = "{\"firstValue\":\"first\",\"secondValue\":\"second\",\"thirdValue\":\"third\"}";
        set.add(new StringFieldModel("firstValue", "first"));
        set.add(new StringFieldModel("secondValue", "second"));
        set.add(new StringFieldModel("thirdValue", "third"));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testEntityModelWithReferenceField() {
        expectedResult = "{\"firstRef\":{\"falseValue\":false,\"trueValue\":true},\"secondRef\":{\"falseValue\":false,\"trueValue\":true}}";
        Set<FieldModel> refSet = new HashSet<>();
        refSet.add(new BooleanFieldModel("trueValue", true));
        refSet.add(new BooleanFieldModel("falseValue", false));
        EntityModel refModel = new EntityModel(refSet);

        set.add(new ReferenceFieldModel("firstRef", refModel));
        set.add(new ReferenceFieldModel("secondRef", refModel));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testEntityModelWithMultiReferenceField() {
        expectedResult = "{\"field\":{\"exceeds_total_count\":false,\"data\":[{\"falseValue\":false,\"trueValue\":true},{\"falseValue\":false,\"trueValue\":true}],\"total_count\":2}}";
        Set<FieldModel> firstSet = new HashSet<>();
        firstSet.add(new BooleanFieldModel("trueValue", true));
        firstSet.add(new BooleanFieldModel("falseValue", false));
        EntityModel firstModel = new EntityModel(firstSet);

        Set<FieldModel> secondSet = new HashSet<>();
        secondSet.add(new StringFieldModel("firstValue", "first"));
        secondSet.add(new StringFieldModel("secondValue", "second"));
        EntityModel secondModel = new EntityModel(firstSet);

        Collection<EntityModel> entityCol = new ArrayList<>();
        entityCol.add(firstModel);
        entityCol.add(secondModel);
        set.add(new MultiReferenceFieldModel("field", entityCol));

        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testEntityModelWithLongField() {
        expectedResult = "{\"secondField\":200,\"firstField\":-200}";
        set.add(new LongFieldModel("firstField", new Long(-200)));
        set.add(new LongFieldModel("secondField", new Long(200)));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testEntityModelWithDateField() {
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z"));
        expectedResult = "{\"field\":\"" + now.toString() + "\"}";
        set.add(new DateFieldModel("field", now));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testErrorModel() {
        expectedResult = "{\"secondField\":651651651,\"firstField\":0}";
        set.add(new LongFieldModel("firstField", new Long(0)));
        set.add(new LongFieldModel("secondField", new Long(651651651)));
        ErrorModel model = new ErrorModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testComplexEntityModel() {
        ZonedDateTime now = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Z"));
        expectedResult = "{\"longField\":200,\"RefField\":{\"falseValue\":false,\"trueValue\":true},\"dateField\":\"" + now.toString() + "\",\"stringField\":\"first\",\"multiRefField\":{\"exceeds_total_count\":false,\"data\":[{\"falseValue\":false,\"trueValue\":true},{\"falseValue\":false,\"trueValue\":true}],\"total_count\":2},\"boolField\":true}";

        Set<FieldModel> refSet = new HashSet<>();
        refSet.add(new BooleanFieldModel("trueValue", true));
        refSet.add(new BooleanFieldModel("falseValue", false));
        EntityModel refModel = new EntityModel(refSet);

        Set<FieldModel> firstSet = new HashSet<>();
        firstSet.add(new BooleanFieldModel("trueValue", true));
        firstSet.add(new BooleanFieldModel("falseValue", false));
        EntityModel firstModel = new EntityModel(firstSet);

        Set<FieldModel> secondSet = new HashSet<>();
        secondSet.add(new StringFieldModel("firstValue", "first"));
        secondSet.add(new StringFieldModel("secondValue", "second"));
        EntityModel secondModel = new EntityModel(firstSet);
        Collection<EntityModel> entityCol = new ArrayList<>();
        entityCol.add(firstModel);
        entityCol.add(secondModel);

        set.add(new MultiReferenceFieldModel("multiRefField", entityCol));
        set.add(new ReferenceFieldModel("RefField", refModel));
        set.add(new DateFieldModel("dateField", now));
        set.add(new LongFieldModel("longField", new Long(200)));
        set.add(new StringFieldModel("stringField", "first"));
        set.add(new BooleanFieldModel("boolField", true));
        model = new EntityModel(set);
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testAddSingleValue() {
        expectedResult = "{\"trueValue\":true}";
        model = new EntityModel();
        model.setValue(new BooleanFieldModel("trueValue", true));
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }

    @Test
    public void testUpdateSingleValue() {
        expectedResult = "{\"trueValue\":true}";
        Set<FieldModel> fieldSet = new HashSet<>();
        fieldSet.add(new BooleanFieldModel("trueValue", false));
        model = new EntityModel(fieldSet);
        model.setValue(new BooleanFieldModel("trueValue", true));
        try {
            JSONObject outJsonEntity = ModelParser.getInstance().getEntityJSONObject(model);
            gotResult = outJsonEntity.toString();
        } catch (Exception ex) {
            fail("Failed with exception: " + ex);
        }
    }
}
