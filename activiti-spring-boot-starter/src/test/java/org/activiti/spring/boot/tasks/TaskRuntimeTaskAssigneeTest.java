package org.activiti.spring.boot.tasks;

import org.activiti.runtime.api.TaskAdminRuntime;
import org.activiti.runtime.api.TaskRuntime;
import org.activiti.runtime.api.model.Task;
import org.activiti.runtime.api.model.builders.TaskPayloadBuilder;
import org.activiti.runtime.api.query.Page;
import org.activiti.runtime.api.query.Pageable;
import org.activiti.runtime.api.security.SecurityManager;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TaskRuntimeTaskAssigneeTest {

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private TaskAdminRuntime taskAdminRuntime;

    @Autowired
    private SecurityManager securityManager;


    @Test
    @WithUserDetails(value = "garth", userDetailsServiceBeanName = "myUserDetailsService")
    public void aCreateStandaloneTaskForAnotherAssignee() {

        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
                .withName("task for salaboy")
                .withAssignee("salaboy")
                .build());

        // the owner should be able to see the created task
        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
                50));

        assertThat(tasks.getContent()).hasSize(1);
        Task task = tasks.getContent().get(0);

        assertThat(task.getAssignee()).isEqualTo("salaboy");
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);


    }

    @Test
    @WithUserDetails(value = "salaboy", userDetailsServiceBeanName = "myUserDetailsService")
    public void bCreateCheckTaskCreatedForSalaboyFromAnotherUser() {

        // the target user should be able to see the task as well
        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
                50));

        assertThat(tasks.getContent()).hasSize(1);
        Task task = tasks.getContent().get(0);

        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        assertThat(task.getAssignee()).isEqualTo(authenticatedUserId);
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);

        Task deletedTask = taskRuntime.delete(TaskPayloadBuilder
                .delete()
                .withTaskId(task.getId())
                .withReason("test clean up")
                .build());

        assertThat(deletedTask).isNotNull();
        assertThat(deletedTask.getStatus()).isEqualTo(Task.TaskStatus.DELETED);

        tasks = taskRuntime.tasks(Pageable.of(0,
                50));
        assertThat(tasks.getContent()).hasSize(0);


    }


    @Test
    @WithUserDetails(value = "garth", userDetailsServiceBeanName = "myUserDetailsService")
    public void cCreateStandaloneTaskForGroupAndClaim() {


        String authenticatedUserId = securityManager.getAuthenticatedUserId();
        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
                                                         .withName("group task")
                                                         .withGroup("doctor")
                                                         .build());

        // the owner should be able to see the created task
        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
                                                         50));

        assertThat(tasks.getContent()).hasSize(1);
        Task task = tasks.getContent().get(0);

        assertThat(task.getAssignee()).isNull();
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);

        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
        assertThat(claimedTask.getAssignee()).isEqualTo(authenticatedUserId);
        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
    }


    @Test
    @WithUserDetails(value = "admin", userDetailsServiceBeanName = "myUserDetailsService")
    public void dCleanUpWithAdmin() {
        Page<Task> tasks = taskAdminRuntime.tasks(Pageable.of(0, 50));
        for (Task t : tasks.getContent()) {
            taskAdminRuntime.delete(TaskPayloadBuilder
                    .delete()
                    .withTaskId(t.getId())
                    .withReason("test clean up")
                    .build());
        }

    }
//
//    @Test
//    public void createStandaloneTaskForGroupAndClaimUnAuthorized() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("group task")
//                                                         .withGroup("doctor")
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        securityManager.authenticate(salaboy);
//        Exception e = null;
//        try {
//            // UnAuthorized Claim
//            Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(NotFoundException.class);
//    }
//
//    @Test
//    public void createStandaloneTaskForGroupAndClaimAuthorized() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("group task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        securityManager.authenticate(salaboy);
//
//        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//        assertThat(claimedTask.getAssignee()).isEqualTo(salaboy.getUsername());
//        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//    }
//
//    @Test
//    public void createStandaloneTaskForGroupAndClaimAndRelease() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("group task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        securityManager.authenticate(salaboy);
//
//        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//        assertThat(claimedTask.getAssignee()).isEqualTo(salaboy.getUsername());
//        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        Task releasedTask = taskRuntime.release(TaskPayloadBuilder.release().withTaskId(claimedTask.getId()).build());
//        assertThat(releasedTask.getAssignee()).isNull();
//        assertThat(releasedTask.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//    }
//
//    @Test
//    public void createStandaloneTaskReleaseUnAuthorized() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("group task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        Exception e = null;
//        try {
//            // UnAuthorized release, task is not assigned
//            Task releasedTask = taskRuntime.release(TaskPayloadBuilder.release().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(IllegalStateException.class);
//    }
//
//    @Test
//    public void createStandaloneTaskAndClaimAndReleaseUnAuthorized() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("group task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        securityManager.authenticate(salaboy);
//
//        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//        assertThat(claimedTask.getAssignee()).isEqualTo(salaboy.getUsername());
//        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        securityManager.authenticate(garth);
//
//        Exception e = null;
//        try {
//            // UnAuthorized release, task is assigned not to you and hence not visible anymore
//            Task releasedTask = taskRuntime.release(TaskPayloadBuilder.release().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(NotFoundException.class);
//    }
//
//    @Test
//    public void createStandaloneTaskAndComplete() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("simple task")
//                                                         .withAssignee(garth.getUsername())
//                                                         .build());
//
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isEqualTo(garth.getUsername());
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        Task completedTask = taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(task.getId()).build());
//        assertThat(completedTask.getStatus()).isEqualTo(Task.TaskStatus.COMPLETED);
//    }
//
//    @Test
//    public void createStandaloneTaskAndCompleteUnAuthorized() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("simple task")
//                                                         .withAssignee(garth.getUsername())
//                                                         .build());
//
//        // the owner should be able to see the created task
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isEqualTo(garth.getUsername());
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        securityManager.authenticate(salaboy);
//        Exception e = null;
//        try {
//            // UnAuthorized Complete
//            Task completedTask = taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(NotFoundException.class);
//    }
//
//    @Test
//    public void createStandaloneTaskAndDelete() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("simple task")
//                                                         .withAssignee(garth.getUsername())
//                                                         .build());
//
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isEqualTo(garth.getUsername());
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        Task deletedTask = taskRuntime.delete(TaskPayloadBuilder.delete().withTaskId(task.getId()).build());
//        assertThat(deletedTask.getStatus()).isEqualTo(Task.TaskStatus.DELETED);
//    }
//
//    @Test
//    public void createStandaloneGroupTaskAndDeleteFail() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("simple task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        Exception e = null;
//        try {
//            // UnAuthorized Delete, task is not assigned
//            Task deletedTask = taskRuntime.delete(TaskPayloadBuilder.delete().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(IllegalStateException.class);
//    }
//
//    @Test
//    public void createStandaloneGroupTaskClaimAndDeleteFail() {
//
//        securityManager.authenticate(garth);
//
//        Task standAloneTask = taskRuntime.create(TaskPayloadBuilder.create()
//                                                         .withName("simple task")
//                                                         .withGroup("activitiTeam")
//                                                         .build());
//
//        Page<Task> tasks = taskRuntime.tasks(Pageable.of(0,
//                                                         50));
//
//        assertThat(tasks.getContent()).hasSize(1);
//        Task task = tasks.getContent().get(0);
//
//        assertThat(task.getAssignee()).isNull();
//        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CREATED);
//
//        securityManager.authenticate(salaboy);
//
//        Task claimedTask = taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(task.getId()).build());
//        assertThat(claimedTask.getAssignee()).isEqualTo(salaboy.getUsername());
//        assertThat(claimedTask.getStatus()).isEqualTo(Task.TaskStatus.ASSIGNED);
//
//        securityManager.authenticate(garth);
//
//        Exception e = null;
//        try {
//            // UnAuthorized Delete, task is not visible, because it was claimed by another user
//            Task deletedTask = taskRuntime.delete(TaskPayloadBuilder.delete().withTaskId(task.getId()).build());
//        } catch (Exception ex) {
//            e = ex;
//        }
//        assertThat(e).isNotNull();
//        assertThat(e).isInstanceOf(NotFoundException.class);
//    }

    //@TODO: add tests for
    //  - Complete task with variables
    //  - Add other users to test group and claim/release combinations
    //  - Add get/set variables tests
    //  - Add Impersonation methods to TaskAdminRuntime
}