package org.cryptomator.hub.persistence.entities;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "device")
public class Device {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false, nullable = false)
    private User user;

    @OneToMany(mappedBy = "device", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Access> access = new HashSet<>();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "publickey", nullable = false)
    private String publickey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Access> getAccess() {
        return access;
    }

    public void setAccess(Set<Access> access) {
        this.access.clear();
        this.access.addAll(access);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublickey() {
        return publickey;
    }

    public void setPublickey(String publickey) {
        this.publickey = publickey;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", user=" + user.getId() +
                ", name='" + name + '\'' +
                ", publickey='" + publickey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return Objects.equals(id, device.id)
                && Objects.equals(user, device.user)
                && Objects.equals(access, device.access)
                && Objects.equals(name, device.name)
                && Objects.equals(publickey, device.publickey);
    }

        /*@Override
    public int hashCode() {
        return Objects.hash(id, user, access, name, publickey);
    }*/

}
