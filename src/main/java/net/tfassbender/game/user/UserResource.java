package net.tfassbender.game.user;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import net.tfassbender.game.ai.GnuGoService;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    UserRepository userRepository;

    @Inject
    GnuGoService gnuGoService;

    @Inject
    JsonWebToken jwt;

    /**
     * Search users by username (excludes bots)
     */
    @GET
    @Path("/search")
    @RolesAllowed("User")
    public Response searchUsers(@QueryParam("q") @DefaultValue("") String query) {
        String currentUser = jwt.getName();
        List<String> usernames = userRepository.findAllUsernames(query).stream()
                .filter(username -> !username.equals(currentUser)) // Exclude current user
                .filter(username -> !gnuGoService.isAiBot(username)) // Exclude AI bots
                .toList();
        return Response.ok(Map.of("users", usernames)).build();
    }

    /**
     * Get list of AI bot usernames sorted by difficulty (easiest to hardest)
     */
    @GET
    @Path("/bots")
    @RolesAllowed("User")
    public Response getAiBots() {
        Map<String, Integer> botDifficulties = gnuGoService.getBotDifficulties();
        List<String> botUsernames = new ArrayList<>(botDifficulties.keySet());
        // Sort by difficulty level (lower level = easier)
        botUsernames.sort((a, b) -> botDifficulties.get(a).compareTo(botDifficulties.get(b)));
        return Response.ok(Map.of("bots", botUsernames)).build();
    }

    /**
     * Get current user's profile
     */
    @GET
    @Path("/me")
    @RolesAllowed("User")
    public Response getCurrentUser(@Context SecurityContext securityContext) {
        String username = jwt.getName();
        Optional<User> userOpt = userService.getUser(username);

        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "User not found"))
                    .build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.username);
        response.put("statistics", user.statistics);
        response.put("createdAt", user.createdAt);

        return Response.ok(response).build();
    }

    /**
     * Get another user's profile (public info only)
     */
    @GET
    @Path("/{username}")
    @RolesAllowed("User")
    public Response getUserProfile(@PathParam("username") String username) {
        Optional<User> userOpt = userService.getUser(username);

        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "User not found"))
                    .build();
        }

        User user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.username);
        response.put("statistics", user.statistics);

        return Response.ok(response).build();
    }
}
