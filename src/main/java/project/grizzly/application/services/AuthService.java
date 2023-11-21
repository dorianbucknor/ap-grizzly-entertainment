package project.grizzly.application.services;

import project.grizzly.application.models.User;
import project.grizzly.server.Request;
import project.grizzly.server.Response;

import java.util.ArrayList;
import java.util.List;

public class AuthService implements IAuth<User> {
    private User user = null;
    private final ArrayList<AuthChangedListener<User>> listeners;
    //    private final CRUDService<User, String> crudService;
    private static AuthService instance;

    private Client client;

    private AuthService() {
        listeners = new ArrayList<>();
        client = Client.getInstance();
//        crudService = new CRUDService<>(User.class); //TODO: change to bean
    }

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    @Override
    public User getLoggedInUser() {
        return user;
    }

    @Override
    public void setLoggedInUser(User user) {
        this.user = user;

        if (user != null) {
            this.user.setLoggedIn(true);
        }

        updateListeners();
    }


    @Override
    public void logOut() {
        setLoggedInUser(null);
        updateListeners();
    }

    @Override
    public User logIn(String email, String password) throws AuthException {
        try {
//            client.getStreams();
            CombinedQuery<User> combinedQuery = new CombinedQuery<User>("SELECT u FROM User u")
                    .where("u.email", "=:email", email);

            client.sendRequest(new Request("READ-WHERE", "USER", combinedQuery));


            Object res = ((Response) client.receiveResponse()).getValue();
//            client();

            List<User> results = (List<User>) res;

            User usr = results == null || results.isEmpty() ? null : results.get(0);

            if (usr != null) {
                if (usr.getPassword().compareTo(password) != 0) {
                    throw new AuthException(AuthCode.INCORRECT_PASSWORD, "Incorrect Password!");
                } else {
                    setLoggedInUser(usr);
                }
            } else {
                throw new AuthException(AuthCode.USER_NOT_FOUND, "User not found!");
            }
            return user;
        } catch (AuthException e) {
            setLoggedInUser(null);
            //todo logging
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);//todo
        }

    }

    private void updateListeners() {
        ArrayList<AuthChangedListener<User>> changedListeners = new ArrayList<>(listeners);
        for (AuthChangedListener<User> listener : changedListeners
        ) {
            if (listener != null) {
                listener.onAuthChanged(user);
            }
        }
    }

    @Override
    public void addAuthChangedListener(AuthChangedListener<User> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeAuthChangedListener(AuthChangedListener<User> listener) {
        listeners.remove(listener);
    }


}
