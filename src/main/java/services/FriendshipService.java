package services;
import java.util.List;

import daos.FriendshipDao;



public class FriendshipService {
    private FriendshipDao friendshipDao;

    public FriendshipService(FriendshipDao friendshipDao) {
        this.friendshipDao = friendshipDao;
    }

    public boolean sendFriendRequest(int requesterId, int addresseeId) {
        if (requesterId == addresseeId) {
            throw new IllegalArgumentException("User can't add themselves");
        }
        if (friendshipDao.areFriends(requesterId, addresseeId)) {
            throw new IllegalArgumentException("Users are already friends");
        }
        return friendshipDao.addFriend(requesterId, addresseeId);
    }

    public boolean acceptFriendRequest(int userId, int requesterId) {
        return friendshipDao.addFriend(userId, requesterId);
    }

    public boolean removeFriend(int userId, int friendId) {
        if (!friendshipDao.areFriends(userId, friendId)) {
            return false;
        }
        return friendshipDao.removeFriend(userId, friendId);
    }

    public List<Integer> getFriends(int userId) {
        return friendshipDao.getFriends(userId);
    }

    public boolean areFriends(int userId, int friendId) {
        return friendshipDao.areFriends(userId, friendId);
    }
}