package com.nixmash.postservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.nixmash.jangles.db.cn.IConnection;
import com.nixmash.jangles.json.JanglesUser;
import com.nixmash.postservice.guice.TestConnection;
import com.nixmash.postservice.guice.UserServiceTestModule;
import com.nixmash.postservice.resource.UserResource;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.test.junit.JettyTestFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static com.nixmash.jangles.utils.JanglesUtils.configureTestDb;
import static com.nixmash.postservice.utils.TestUtils.TEST_URL;
import static com.nixmash.postservice.utils.TestUtils.YAML_CONFIG;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RestTest {

    @ClassRule
    public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

    @Inject
    private UserResource userResource;

    private Client client;

// region @BeforeClass and @AfterClass

    @BeforeClass
    public static void setupClass() {
        try {
            configureTestDb("populate.sql");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }

        JETTY_FACTORY.app()
                .autoLoadModules()
                .args(YAML_CONFIG)
                .module(binder -> binder.bind(IConnection.class).to(TestConnection.class))
                .module(binder -> JerseyModule.extend(binder).addResource(UserResource.class))
                .start();

    }

    @Before
    public void startJetty() {
        Injector injector = Guice.createInjector(new UserServiceTestModule());
        injector.injectMembers(this);

        ClientConfig config = new ClientConfig();
        this.client = ClientBuilder.newClient(config);
    }

    // endregion

    @Test
    public void getUserTest() {
        WebTarget target = client.target(TEST_URL + "/users/1");
        Response response = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JanglesUser janglesUser = response.readEntity(JanglesUser.class);
        assertTrue(janglesUser.getUserId().equals(1L));
        response.close();
    }

    @Test
    public void getAllUsersTest() throws IOException {
        WebTarget target = client.target(TEST_URL + "/users");
        Response response = target.request().get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ObjectMapper mapper = new ObjectMapper();
        List<JanglesUser> users = mapper.readValue(response.readEntity(String.class),
                TypeFactory
                        .defaultInstance()
                        .constructCollectionType(List.class, JanglesUser.class));
        assertThat(users.size(), Matchers.greaterThan(1));
        response.close();
    }
}