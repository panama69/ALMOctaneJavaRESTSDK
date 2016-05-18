package com.hpe.adm.nga.sdk;

import com.hpe.adm.nga.sdk.authorisation.BasicAuthorisation;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.*;


@PowerMockIgnore("javax.management.*")
@RunWith(PowerMockRunner.class)
public class TestGetEntity{

	private static EntityListService service = null;
	private static EntityListService spiedService = null;
	private static EntityListService.Get spiedGetEntity = null;
	private static NGA nga;
	private static String MY_APP_ID = "moris@korentec.co.il";
	private static String MY_APP_SECRET = "Moris4095";
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		nga = new NGA(requestFactory, "https://mqast001pngx.saas.hpe.com", "4063", 1002);
		
		nga = (new NGA.Builder(
				new BasicAuthorisation(){
					@Override
					public String getUsername(){
						
						return MY_APP_ID;
					}
					
					@Override
					public String getPassword(){
						
						return MY_APP_SECRET;
					}
					
				}
				)).Server("https://mqast001pngx.saas.hpe.com").sharedSpace(4063).workSpace(1002).build();
		
		
		EntityList defects = nga.entityList("defects");
		spiedGetEntity = PowerMockito.spy(defects.get());
		EntityList spiedDefects = PowerMockito.spy(defects);
		service = (EntityListService)Whitebox.getInternalState(spiedDefects, "entityListService");					
		spiedService = PowerMockito.spy(service);	
	}

	/**
	 * Test correct build of URL with field specification, limits, offset and orderBy.
	 * The method invokes internal protected method with retrieved private parameters.
	 */
	@Test
	public void testUrlBuild(){
		final String expectedResult = "https://mqast001pngx.saas.hpe.com/api/shared_spaces/4063/workspaces/1002/defects?fields=version_stamp,item_type&limit=10&offset=1&order_by=-version_stamp";
		try{					
			spiedGetEntity.addFields("version_stamp", "item_type").limit(10).offset(1).addOrderBy("version_stamp",false);
			// get Internal fields values and call internal protected method			
			String urlDomain = (String)Whitebox.getInternalState(spiedService, "urlDomain");
			String fieldsParams = (String) Whitebox.getInternalState(spiedGetEntity, "fieldsParams");
			String orderByParam = (String) Whitebox.getInternalState(spiedGetEntity, "orderByParam");
			long limitParam = (Long) Whitebox.getInternalState(spiedGetEntity, "limitParam");
			long ofsetParam = (Long) Whitebox.getInternalState(spiedGetEntity, "ofsetParam");
			Query queryParams = (Query) Whitebox.getInternalState(spiedGetEntity, "queryParams");						
			
			Method urlBuilder = service.getClass().getDeclaredMethod("urlBuilder", new Class[] {String.class, String.class, String.class, long.class, long.class, Query.class});
			urlBuilder.setAccessible(true);
			String url = (String)urlBuilder.invoke(service, urlDomain, fieldsParams, orderByParam, limitParam, ofsetParam, queryParams);
			assertEquals(expectedResult, url);
		}
		catch(Exception ex){
			fail("Failed with exception: " + ex);
		}
	}

	/**
	 * Test inequality between the expected URL and the created one. 
	 * The method invokes internal protected method with retrieved private parameters.
	 */
	@Test
	public void testUrlBuildFailure(){
		final String expectedFalseResult = "https://mqast001pngx.saas.hpe.com/api/shared_spaces/4063/workspaces/1002/defects?fields=version_stamp,item_type&limit=10&offset=1&order_by=-version_stamp";
		try{					
			spiedGetEntity.addFields("version_stamp", "item_type", "id").limit(10).offset(1).addOrderBy("version_stamp",true); // Field was added, and orderBy was changed to TRUE value
			// get Internal fields values and call internal protected method			
			String urlDomain = (String)Whitebox.getInternalState(spiedService, "urlDomain");
			String fieldsParams = (String) Whitebox.getInternalState(spiedGetEntity, "fieldsParams");
			String orderByParam = (String) Whitebox.getInternalState(spiedGetEntity, "orderByParam");
			long limitParam = (Long) Whitebox.getInternalState(spiedGetEntity, "limitParam");
			long ofsetParam = (Long) Whitebox.getInternalState(spiedGetEntity, "ofsetParam");
			Query queryParams = (Query) Whitebox.getInternalState(spiedGetEntity, "queryParams");						
			
			Method urlBuilder = service.getClass().getDeclaredMethod("urlBuilder", new Class[] {String.class, String.class, String.class, long.class, long.class, Query.class});
			urlBuilder.setAccessible(true);
			String url = (String)urlBuilder.invoke(service, urlDomain, fieldsParams, orderByParam, limitParam, ofsetParam, queryParams);
			assertFalse(expectedFalseResult.equals(url));
		}
		catch(Exception ex){
			fail("Failed with exception: " + ex);
		}
	}
}
