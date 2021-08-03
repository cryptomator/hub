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
import java.util.stream.Collectors;

@Entity
@Table(name = "vault")
public class Vault {

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

    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "iterations", nullable = false)
    private String iterations;

    @Column(name = "masterkey", nullable = false)
    private String masterkey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getIterations() {
        return iterations;
    }

    public void setIterations(String iterations) {
        this.iterations = iterations;
    }

    public String getMasterkey() {
        return masterkey;
    }

    public void setMasterkey(String masterkey) {
        this.masterkey = masterkey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vault vault = (Vault) o;
        return Objects.equals(id, vault.id)
                && Objects.equals(user, vault.user)
                && Objects.equals(access, vault.access)
                && Objects.equals(name, vault.name)
                && Objects.equals(salt, vault.salt)
                && Objects.equals(iterations, vault.iterations)
                && Objects.equals(masterkey, vault.masterkey);
    }

    /*@Override
    public int hashCode() {
        return Objects.hash(id, user, access, name, salt, costParam, masterkey);
    }*/

    @Override
    public String toString() {
        return "Vault{" +
                "id='" + id + '\'' +
                ", user=" + user +
                ", access=" + access.stream().map(Access::getId).collect(Collectors.toList()) +
                ", name='" + name + '\'' +
                ", salt='" + salt + '\'' +
                ", iterations='" + iterations + '\'' +
                ", masterkey='" + masterkey + '\'' +
                '}';
    }
}
