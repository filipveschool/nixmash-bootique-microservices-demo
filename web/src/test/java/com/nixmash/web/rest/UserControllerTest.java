package com.nixmash.web.rest;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.nixmash.jangles.db.IConnection;
import com.nixmash.jangles.db.UsersDb;
import com.nixmash.jangles.db.UsersDbImpl;
import com.nixmash.web.auth.NixmashRealm;
import com.nixmash.web.controller.UserController;
import com.nixmash.web.guice.GuiceJUnit4Runner;
import com.nixmash.web.guice.TestConnection;
import com.nixmash.web.guice.WebTestModule;
import com.nixmash.web.resolvers.TemplatePathResolver;
import com.nixmash.web.service.UserService;
import com.nixmash.web.service.UserServiceImpl;
import io.bootique.jersey.JerseyModule;
import io.bootique.jetty.test.junit.JettyTestFactory;
import io.bootique.shiro.ShiroModule;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.nixmash.jangles.utils.JanglesUtils.configureTestDb;
import static com.nixmash.web.utils.TestUtils.TEST_URL;
import static com.nixmash.web.utils.TestUtils.YAML_CONFIG;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by daveburke on 7/1/17.
 */
@RunWith(GuiceJUnit4Runner.class)
public class UserControllerTest {

    @ClassRule
    public static JettyTestFactory JETTY_FACTORY = new JettyTestFactory();

    @Inject
    private UserController mockUserController;

    @Inject
    private com.nixmash.web.service.UserServiceImpl userService;

    @Inject
    private UsersDbImpl usersDb;

    @Inject
    private TestConnection iConnection;

    @Inject
    private TemplatePathResolver templatePathResolver;

    private Client client;

    private Answer<String> usersAnswer = new Answer<String>() {
        public String answer(InvocationOnMock invocation) throws Throwable {
            Map<String, Object> model = new HashMap<>();
            model.put("users", new ArrayList<>());
            return templatePathResolver.populateTemplate("users.html", model);
        }
    };

    @BeforeClass
    public static void setupClass() {
        try {
            configureTestDb("populate.sql");
        } catch (FileNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        JETTY_FACTORY.app()
                .autoLoadModules()
                .args(YAML_CONFIG)
                .module(binder -> JerseyModule.extend(binder).addResource(UserController.class))
                .module(b -> b.bind(IConnection.class).to(TestConnection.class))
                .module(b -> b.bind(UserService.class).to(UserServiceImpl.class))
                .module(b -> b.bind(UsersDb.class).to(UsersDbImpl.class))
                .module(b -> ShiroModule.extend(b).addRealm(NixmashRealm.class))
                .start();

    }

    @AfterClass
    public static void tearDown() {
        try {
            configureTestDb("clear.sql");
        } catch (FileNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void startJetty() {

        // region Guice injection and Jersey Client config

        Injector injector = Guice.createInjector(new WebTestModule());
        injector.injectMembers(this);

        ClientConfig config = new ClientConfig();
        this.client = ClientBuilder.newClient(config);

        // endregion

        this.mockUserController = Mockito.mock(UserController.class);
        when(mockUserController.restUsers()).thenAnswer(usersAnswer);
    }

    /**
     *
     *  Normal access to "/users" will display the error.html page
     */
    @Test
    public void getUsersTest() throws Exception {
        WebTarget target = client.target(TEST_URL + "/users");
        Response response = target.request().get();
        assertTrue(response.readEntity(String.class).contains("Oops!"));
    }

    /**
     *
     * UserController is mocked so method returns Stubbed Answer
     *  displaying users.html page
     */
    @Test
    public void usersPageDisplays() throws Exception {
        String populatedTemplate = mockUserController.restUsers();
        assertTrue(populatedTemplate.contains("<title>Users Test Page</title>"));
    }

}
