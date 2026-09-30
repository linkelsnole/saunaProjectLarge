package com.example.modbus_api.service;
import com.example.modbus_api.dto.UserManagementDto.*;
import com.example.modbus_api.security.model.*;
import com.example.modbus_api.security.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserManagementServiceTest {
 UserRepository users;PasswordEncoder encoder;UserManagementService service;AppUser actor,target;
 @BeforeEach void setup(){users=mock(UserRepository.class);encoder=mock(PasswordEncoder.class);PlatformTransactionManager manager=mock(PlatformTransactionManager.class);when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());service=new UserManagementService(users,encoder,manager,mock(com.example.modbus_api.security.repository.RefreshTokenRepository.class),mock(DeletedUsers.class));actor=AppUser.builder().id(1L).username("actor").role(Role.ROLE_ADMIN).build();target=AppUser.builder().id(2L).username("target").role(Role.ROLE_USER).build();when(users.findByUsername("actor")).thenReturn(Optional.of(actor));when(users.findById(2L)).thenReturn(Optional.of(target));when(users.saveAndFlush(any())).thenAnswer(a->a.getArgument(0));}
 @Test void refusesStaleActorEvenWhenRequestWasPreviouslyAuthorized(){actor.setRole(Role.ROLE_READONLY);assertThrows(AccessDeniedException.class,()->service.changeRole("actor",2,new ChangeRole(Role.ROLE_ADMIN,Role.ROLE_USER)));verify(users,never()).saveAndFlush(any());}
 @Test void refusesLastAdministratorDemotion(){target.setRole(Role.ROLE_ADMIN);when(users.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);assertThrows(IllegalArgumentException.class,()->service.changeRole("actor",2,new ChangeRole(Role.ROLE_USER,Role.ROLE_ADMIN)));verify(users,never()).saveAndFlush(any());}
 @Test void keepsPasswordAndIdentityWhenChangingRole(){target.setPassword("existing-hash");service.changeRole("actor",2,new ChangeRole(Role.ROLE_READONLY,Role.ROLE_USER));assertEquals("existing-hash",target.getPassword());assertEquals("target",target.getUsername());assertEquals(Role.ROLE_READONLY,target.getRole());verifyNoInteractions(encoder);}
 @Test void refusesConflictBeforeSaving(){assertThrows(ConcurrentModificationException.class,()->service.changeRole("actor",2,new ChangeRole(Role.ROLE_ADMIN,Role.ROLE_READONLY)));verify(users,never()).saveAndFlush(any());}
 @Test void createsOnlyRequestedAccountAndHashesPassword(){when(encoder.encode("synthetic-test-password")).thenReturn("encoded");service.create("actor",new Create("tester_new","synthetic-test-password",Role.ROLE_TESTER));org.mockito.ArgumentCaptor<AppUser> saved=org.mockito.ArgumentCaptor.forClass(AppUser.class);verify(users).saveAndFlush(saved.capture());assertEquals("encoded",saved.getValue().getPassword());assertEquals(Role.ROLE_TESTER,saved.getValue().getRole());}
 @Test void cannotDeleteLastAdministrator(){target.setRole(Role.ROLE_ADMIN);when(users.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);assertThrows(IllegalArgumentException.class,()->service.delete("actor",2L));verify(users,never()).delete(any());}
}
