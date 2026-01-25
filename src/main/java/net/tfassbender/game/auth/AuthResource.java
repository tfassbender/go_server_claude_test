package net.tfassbender.game.auth;

import net.tfassbender.game.user.User;
import net.tfassbender.game.user.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuthResource.class);

    @Inject
    UserService userService;

    @Inject
    JwtService jwtService;

    /**
     * Register a new user
     */
    @POST
    @Path("/register")
    public Response register(RegisterRequest request) {
        try {
            // Validate request
            if (request.username == null || request.password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new AuthResponse("Username and password are required"))
                        .build();
            }

            // Register user
            User user = userService.registerUser(request.username, request.password);

            return Response.status(Response.Status.CREATED)
                    .entity(new AuthResponse("User created successfully"))
                    .build();

        } catch (IllegalArgumentException e) {
            LOG.debug("Registration failed: {}", e.getMessage());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new AuthResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Error during registration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new AuthResponse("Registration failed"))
                    .build();
        }
    }

    /**
     * Login and receive JWT token
     */
    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            // Validate request
            if (request.username == null || request.password == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Username and password are required"))
                        .build();
            }

            // Authenticate user
            Optional<User> userOpt = userService.authenticateUser(request.username, request.password);

            if (userOpt.isEmpty()) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("error", "Invalid username or password"))
                        .build();
            }

            // Generate JWT token
            String token = jwtService.generateToken(request.username);

            return Response.ok(new AuthResponse(token, request.username)).build();

        } catch (Exception e) {
            LOG.error("Error during login", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Login failed"))
                    .build();
        }
    }
}
