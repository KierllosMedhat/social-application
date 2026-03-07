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
    if (friendshipDao.friendshipExists(requesterId, addresseeId)) {
      throw new IllegalArgumentException("A friendship or request already exists");
    }
    return friendshipDao.sendFriendRequest(requesterId, addresseeId);
  }

  public boolean acceptFriendRequest(int requesterId, int addresseeId) {
    return friendshipDao.acceptFriendRequest(requesterId, addresseeId);
  }

  public boolean rejectFriendRequest(int requesterId, int addresseeId) {
    return friendshipDao.rejectFriendRequest(requesterId, addresseeId);
  }

  public boolean cancelFriendRequest(int requesterId, int addresseeId) {
    return friendshipDao.cancelFriendRequest(requesterId, addresseeId);
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

  public List<Integer> getPendingRequests(int userId) {
    return friendshipDao.getPendingRequests(userId);
  }

  public boolean areFriends(int userId, int friendId) {
    return friendshipDao.areFriends(userId, friendId);
  }

  public boolean friendshipExists(int userId, int otherId) {
    return friendshipDao.friendshipExists(userId, otherId);
  }
}
