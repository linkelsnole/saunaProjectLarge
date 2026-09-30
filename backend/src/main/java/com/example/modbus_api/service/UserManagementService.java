package com.example.modbus_api.service;
import com.example.modbus_api.dto.UserManagementDto.*;
import com.example.modbus_api.security.model.*;
import com.example.modbus_api.security.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserManagementService {
    private final com.example.modbus_api.security.repository.RefreshTokenRepository sessions;
    private final DeletedUsers deleted;
    private final PasswordChanges passwordChanges;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    public UserManagementService(UserRepository users,PasswordEncoder encoder,PlatformTransactionManager manager,com.example.modbus_api.security.repository.RefreshTokenRepository sessions,DeletedUsers deleted,PasswordChanges passwordChanges) {
        this.passwordChanges=passwordChanges;this.sessions=sessions;this.deleted=deleted;
        this.users=users;this.encoder=encoder;this.transaction=new TransactionTemplate(manager);
    }
    public List<UserView> list() {return users.findAll(Sort.by("username")).stream().map(this::view).collect(Collectors.toList());}
    public synchronized UserView create(String actor,Create request) {
        return transaction.execute(status->{
            requireAdmin(actor);
            if(request.getUsername()==null || !request.getUsername().matches("[A-Za-z0-9_.-]{3,32}")) throw new IllegalArgumentException("Логин: 3–32 латинских символа, цифры, точка, дефис или подчёркивание");
            if(request.getPassword()==null || request.getPassword().length()<12 || request.getPassword().getBytes(StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Пароль: не менее 12 символов и не более 72 байт UTF-8");
            if(request.getRole()==null) throw new IllegalArgumentException("Выберите роль");
            if(users.existsByUsernameIgnoreCase(request.getUsername())) throw new ConcurrentModificationException("Такой логин уже занят");
            return view(users.saveAndFlush(AppUser.builder().username(request.getUsername()).password(encoder.encode(request.getPassword())).role(request.getRole()).build()));
        });
    }
    public synchronized UserView changeRole(String actor,long id,ChangeRole request) {
        // Keep the transaction commit inside the monitor so parallel demotions cannot remove every admin.
        return transaction.execute(status->{
            requireAdmin(actor);
            AppUser user=users.findById(id).orElseThrow(()->new NoSuchElementException("Пользователь не найден"));
            if(request.getRole()==null || request.getExpectedRole()==null) throw new IllegalArgumentException("Выберите роль");
            if(user.getRole()!=request.getExpectedRole()) throw new ConcurrentModificationException("Роль уже изменена другим администратором. Обновите список.");
            if(user.getRole()==request.getRole()) return view(user);
            if(user.getUsername().equals(actor)) throw new IllegalArgumentException("Нельзя изменить собственную роль");
            if(user.getRole()==Role.ROLE_ADMIN && users.countByRole(Role.ROLE_ADMIN)<=1) throw new IllegalArgumentException("Нельзя убрать последнего администратора");
            user.setRole(request.getRole());
            return view(users.saveAndFlush(user));
        });
    }
    public synchronized void changePassword(String actor,long id,ChangePassword request) {
        transaction.execute(status->{
            requireAdmin(actor);
            String password=request.getPassword();
            if(password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
                throw new IllegalArgumentException("Пароль: не менее 12 символов и не более 72 байт UTF-8");
            AppUser user=users.findById(id).orElseThrow(()->new NoSuchElementException("Пользователь не найден"));
            user.setPassword(encoder.encode(password));
            users.saveAndFlush(user);
            sessions.deleteByUserId(id);sessions.flush();
            passwordChanges.record(user.getUsername());
            return null;
        });
    }
    public synchronized void delete(String actor,long id) {
        transaction.execute(status->{
            requireAdmin(actor);
            AppUser user=users.findById(id).orElseThrow(()->new NoSuchElementException("Пользователь не найден"));
            if(user.getUsername().equals(actor)) throw new IllegalArgumentException("Нельзя удалить собственную учётную запись");
            if(user.getRole()==Role.ROLE_ADMIN && users.countByRole(Role.ROLE_ADMIN)<=1)
                throw new IllegalArgumentException("Нельзя удалить последнего администратора");
            sessions.deleteByUserId(id);
            sessions.flush();
            deleted.record(user.getUsername());
            users.delete(user);
            users.flush();
            return null;
        });
    }
    private void requireAdmin(String username) {
        AppUser actor=users.findByUsername(username).orElseThrow(()->new AccessDeniedException("Нет доступа"));
        if(actor.getRole()!=Role.ROLE_ADMIN) throw new AccessDeniedException("Требуются права администратора");
    }
    private UserView view(AppUser user) {return new UserView(user.getId(),user.getUsername(),user.getRole());}
}
