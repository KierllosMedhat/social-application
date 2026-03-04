package models;

import java.time.LocalDateTime;

public class Friendship {
    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    int friendshipId;
    int requesterId;
    int addresseeId;
    Status status;
    LocalDateTime createdAt;

    // for handling frameworks
    public Friendship() {
    }

    // the constructor for creating a new friendship request (only requester and addressee, status defaults to PENDING)
    public Friendship(int requesterId, int addresseeId) {
        if (requesterId == addresseeId) {
            throw new IllegalArgumentException("User can't add himself");
        }
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = Status.PENDING; // default status for new requests
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for fetching from Database (with all fields)
    public Friendship(int friendshipId, int requesterId, int addresseeId, Status status, LocalDateTime createdAt) {
        this.friendshipId = friendshipId;
        this.requesterId = requesterId;
        this.addresseeId = addresseeId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(int friendshipId) {
        this.friendshipId = friendshipId;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(int requesterId) {
        this.requesterId = requesterId;
    }

    public int getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(int addresseeId) {
        this.addresseeId = addresseeId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
