package org.activiti.web.simple.webapp.service.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.web.simple.webapp.model.Leave;
import org.activiti.web.simple.webapp.service.LeaveService;
import org.activiti.web.simple.webapp.service.LeaveWorkFlowService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ctc.wstx.util.StringUtil;

import net.sf.json.JSONObject;

@Service("leaveWorkFlowServiceImpl")
@Transactional(propagation = Propagation.REQUIRED)
public class LeaveWorkFlowServiceImpl implements LeaveWorkFlowService {

	@Resource(name = "leaveServiceImpl")
	private LeaveService leaveService;

	@SuppressWarnings("unused")
	@Resource(name = "identityService")
	private IdentityService identityService;

	@Resource(name = "runtimeService")
	private RuntimeService runtimeService;

	@Resource(name = "historyService")
	private HistoryService historyService;

	@Resource(name = "taskService")
	private TaskService taskService;

	@SuppressWarnings("unused")
	@Resource(name = "managementService")
	private ManagementService managementService;

	@SuppressWarnings("unused")
	@Resource(name = "formService")
	private FormService formService;

	@Resource(name = "repositoryService")
	private RepositoryService repositoryService;

	/**
	 * 启动工作流
	 */
	public ProcessInstance startWorkflow(String key, String businessKey,
			Map<String, Object> variables) {
		// 根据流程定义的key启动工作流

		ProcessInstance processInstance = runtimeService
				.startProcessInstanceByKey(key, businessKey, variables);

		return processInstance;
	}

	/**
	 * 根据用户Id查询待办任务列表
	 * 
	 * @param userid
	 *            用户id
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findRestTask(String userid, String processDefinitionKey, int firstResult, int maxResults) {

		// 存放当前用户的所有任务
		List<Task> tasks = new ArrayList<Task>();

		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();

		// 根据当前用户的id查询代办任务列表(已经签收)
		List<Task> taskAssignees = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)
				.taskAssignee(userid).orderByTaskPriority().desc()
				.orderByTaskCreateTime().desc().listPage(firstResult, maxResults);
		
		// 根据当前用户id查询未签收的任务列表
		List<Task> taskCandidates = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)		
				.taskCandidateUser(userid).orderByTaskPriority().desc()
				.orderByTaskCreateTime().desc().listPage(firstResult, maxResults);

		tasks.addAll(taskAssignees);// 添加已签收准备执行的任务(已经分配到任务的人)
		tasks.addAll(taskCandidates);// 添加还未签收的任务(任务的候选者)

		// 遍历所有的任务列表,关联实体
		for (Task task : tasks) {
			//map
			JSONObject jo = new JSONObject();
			String processInstanceId = task.getProcessInstanceId();
			// 根据流程实例id查询流程实例
			ProcessInstance processInstance = runtimeService
					.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			// 获取业务id
			String businessKey = processInstance.getBusinessKey();
			jo.put("businessKey", businessKey);
			// 设置属性
			JSONObject taskObject = new JSONObject();
			taskObject.put("taskId", task.getId());
			taskObject.put("taskName", task.getName());
			jo.put("task", taskObject);
			// 流程实例
			jo.put("processId", processInstance.getId());
			// 流程定义ID
/*			leave.setTask(task);
			leave.setProcessInstance(processInstance);
			leave.setProcessInstanceId(processInstance.getId());
			leave.setProcessDefinition(getProcessDefinition(processInstance
					.getProcessDefinitionId()));*/
			leaves.add(jo);
		}

		return leaves;
	}
	
	/**
	 * 根据用户Id数组查询待办任务列表
	 * Added by MingLiang on 2017.7.25
	 * 
	 * @param userids
	 *            用户id数组
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findRestTask(String[] userids, String processDefinitionKey, int firstResult, int maxResults) {

		// 存放当前用户的所有任务
		List<Task> tasks = new ArrayList<Task>();

		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();
		
		for(int i = 0; i < userids.length; i++){
			// 根据当前用户的id查询代办任务列表(已经签收)
			List<Task> taskAssignees = taskService.createTaskQuery()
					.processDefinitionKey(processDefinitionKey)
					.taskAssignee(userids[i]).orderByTaskPriority().desc()
					.orderByTaskCreateTime().desc().list();//.listPage(firstResult, maxResults);
			
			// 根据当前用户id查询未签收的任务列表
			List<Task> taskCandidates = taskService.createTaskQuery()
					.processDefinitionKey(processDefinitionKey)		
					.taskCandidateUser(userids[i]).orderByTaskPriority().desc()
					.orderByTaskCreateTime().desc().list();//.listPage(firstResult, maxResults);
			tasks.addAll(taskAssignees);// 添加已签收准备执行的任务(已经分配到任务的人)
			tasks.addAll(taskCandidates);// 添加还未签收的任务(任务的候选者)
			
		}
		tasks = (tasks.size() > maxResults) ? tasks.subList(firstResult, maxResults) : tasks.subList(firstResult, tasks.size());
		// 遍历所有的任务列表,关联实体
		for (Task task : tasks) {
			//map
			JSONObject jo = new JSONObject();
			String processInstanceId = task.getProcessInstanceId();
			// 根据流程实例id查询流程实例
			ProcessInstance processInstance = runtimeService
					.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			// 获取业务id
			String businessKey = processInstance.getBusinessKey();
			jo.put("businessKey", businessKey);
			// 设置属性
			JSONObject taskObject = new JSONObject();
			taskObject.put("taskId", task.getId());
			taskObject.put("taskName", task.getName());
			jo.put("task", taskObject);
			// 流程实例
			jo.put("processId", processInstance.getId());
			// 流程定义ID
/*			leave.setTask(task);
			leave.setProcessInstance(processInstance);
			leave.setProcessInstanceId(processInstance.getId());
			leave.setProcessDefinition(getProcessDefinition(processInstance
					.getProcessDefinitionId()));*/
			leaves.add(jo);
		}

		return leaves;
	}
	
	@Override
	@Transactional(readOnly=true)
	public int findRestTaskCount(String userid, String processDefinitionKey) {
		// TODO Auto-generated method stub
		// 存放当前用户的所有任务
		List<Task> tasks = new ArrayList<Task>();

		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();

		// 根据当前用户的id查询代办任务列表(已经签收)
		List<Task> taskAssignees = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)
				.taskAssignee(userid).list();
		// 根据当前用户id查询未签收的任务列表
		List<Task> taskCandidates = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)		
				.taskCandidateUser(userid).list();

		tasks.addAll(taskAssignees);// 添加已签收准备执行的任务(已经分配到任务的人)
		tasks.addAll(taskCandidates);// 添加还未签收的任务(任务的候选者)

		return tasks.size();
	}
	
	/**
	 * 
	 * 根据用户id数据获取所有待办条数
	 * Added by MingLiang on 2017.7.25
	 * 
	 * */
	@Override
	@Transactional(readOnly=true)
	public int findRestTaskCount(String[] userids, String processDefinitionKey) {
		// TODO Auto-generated method stub
		// 存放当前用户的所有任务
		List<Task> tasks = new ArrayList<Task>();

		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();
		for(int i = 0; i < userids.length; i++){
			// 根据当前用户的id查询代办任务列表(已经签收)
			List<Task> taskAssignees = taskService.createTaskQuery()
					.processDefinitionKey(processDefinitionKey)
					.taskAssignee(userids[i]).list();
			// 根据当前用户id查询未签收的任务列表
			List<Task> taskCandidates = taskService.createTaskQuery()
					.processDefinitionKey(processDefinitionKey)		
					.taskCandidateUser(userids[i]).list();

			tasks.addAll(taskAssignees);// 添加已签收准备执行的任务(已经分配到任务的人)
			tasks.addAll(taskCandidates);// 添加还未签收的任务(任务的候选者)
		}
		
		return tasks.size();
	}
	
	/**
	 * 根据用户Id查询待办任务列表
	 * 
	 * @param userid
	 *            用户id
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Leave> findTask(String userid, String processDefinitionKey) {

		// 存放当前用户的所有任务
		List<Task> tasks = new ArrayList<Task>();

		List<Leave> leaves = new ArrayList<Leave>();

		// 根据当前用户的id查询代办任务列表(已经签收)
		List<Task> taskAssignees = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)
				.taskAssignee(userid).orderByTaskPriority().desc()
				.orderByTaskCreateTime().desc().list();
		// 根据当前用户id查询未签收的任务列表
		List<Task> taskCandidates = taskService.createTaskQuery()
				.processDefinitionKey(processDefinitionKey)
				.taskCandidateUser(userid).orderByTaskPriority().desc()
				.orderByTaskCreateTime().desc().list();

		tasks.addAll(taskAssignees);// 添加已签收准备执行的任务(已经分配到任务的人)
		tasks.addAll(taskCandidates);// 添加还未签收的任务(任务的候选者)

		// 遍历所有的任务列表,关联实体
		for (Task task : tasks) {
			String processInstanceId = task.getProcessInstanceId();
			// 根据流程实例id查询流程实例
			ProcessInstance processInstance = runtimeService
					.createProcessInstanceQuery()
					.processInstanceId(processInstanceId).singleResult();
			// 获取业务id
			String businessKey = processInstance.getBusinessKey();
			// 查询请假实体
			Leave leave = leaveService.findById(businessKey);
			// 设置属性
			leave.setTask(task);
			leave.setProcessInstance(processInstance);
			leave.setProcessInstanceId(processInstance.getId());
			leave.setProcessDefinition(getProcessDefinition(processInstance
					.getProcessDefinitionId()));

			leaves.add(leave);
		}

		return leaves;
	}

	/**
	 * 查询运行中的流程实例
	 * 
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Leave> findRunningProcessInstaces(String processDefinitionKey) {
		List<Leave> leaves = new ArrayList<Leave>();

		List<ProcessInstance> processInstances = runtimeService
				.createProcessInstanceQuery()
				.processDefinitionKey(processDefinitionKey).list();

		// 关联业务实体
		for (ProcessInstance processInstance : processInstances) {

			String businessKey = processInstance.getBusinessKey();

			Leave leave = leaveService.findById(businessKey);

			leave.setProcessInstance(processInstance);
			leave.setProcessInstanceId(processInstance.getId());
			leave.setProcessDefinition(getProcessDefinition(processInstance
					.getProcessDefinitionId()));

			// 设置当前任务信息
			// 根据流程实例id,按照任务创建时间降序排列,查询一条任务信息
			List<Task> tasks = taskService.createTaskQuery()
					.processInstanceId(processInstance.getId())
					.orderByTaskCreateTime().desc().listPage(0, 1);
			leave.setTask(tasks.get(0));

			leaves.add(leave);
		}

		return leaves;
	}
	
	/**
	 * 查询运行中的流程实例
	 * 
	 * @param processDefinitionKey
	 *            流程定义的key
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findRunningProcessInstances(String processDefinitionKey) {
		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();

		List<ProcessInstance> processInstances = runtimeService
				.createProcessInstanceQuery()
				.processDefinitionKey(processDefinitionKey).list();

		// 关联业务实体
		for (ProcessInstance processInstance : processInstances) {
			JSONObject jo = new JSONObject();
			String businessKey = processInstance.getBusinessKey();

			// 设置当前任务信息
			// 根据流程实例id,按照任务创建时间降序排列,查询一条任务信息
			List<Task> tasks = taskService.createTaskQuery()
					.processInstanceId(processInstance.getId())
					.orderByTaskCreateTime().desc().listPage(0, 1);
			jo.put("businessKey", businessKey);
			// 设置属性
			JSONObject taskObject = new JSONObject();
			taskObject.put("taskId", tasks.get(0).getId());
			taskObject.put("taskName", tasks.get(0).getName());
			jo.put("task", taskObject);
			leaves.add(jo);
		}

		return leaves;
	}

	/**
	 * 查询已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Leave> findFinishedProcessInstaces(String processDefinitionKey) {

		List<Leave> leaves = new ArrayList<Leave>();

		// 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
		List<HistoricProcessInstance> list = historyService
				.createHistoricProcessInstanceQuery().finished()
				.processDefinitionKey(processDefinitionKey).list();

		// 关联业务实体
		for (HistoricProcessInstance historicProcessInstance : list) {

			String businessKey = historicProcessInstance.getBusinessKey();

			Leave leave = leaveService.findById(businessKey);

			leave.setHistoricProcessInstance(historicProcessInstance);
			leave.setProcessDefinition(getProcessDefinition(historicProcessInstance
					.getProcessDefinitionId()));

			leaves.add(leave);
		}

		return leaves;
	}
	
	/**
	 * 查询已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findFinishedProcessInstances(String processDefinitionKey) {

		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();

		// 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
		List<HistoricProcessInstance> list = historyService
				.createHistoricProcessInstanceQuery().finished()
				.processDefinitionKey(processDefinitionKey).list();

		// 关联业务实体
		for (HistoricProcessInstance historicProcessInstance : list) {
			JSONObject jo = new JSONObject();
			String businessKey = historicProcessInstance.getBusinessKey();
			jo.put("businessKey", businessKey);
			// 设置属性
			JSONObject taskObject = new JSONObject();
			taskObject.put("historyId", historicProcessInstance.getId());
			taskObject.put("historyName", historicProcessInstance.getName());
			jo.put("history", taskObject);
			leaves.add(jo);
		}

		return leaves;
	}
	
	/**
	 * 查询指定用户参与已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findFinishedProcessInstancesByUser (
			String userId, 
			int firstResult, 
			int maxResults,
			String beginTime,
			String endTime,
			String businessType) {

		List<HistoricProcessInstance> list = null;
		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();
		SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
		try{
			
		 // 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
		    if (("".equals(beginTime) || beginTime==null) && ("".equals(endTime) || endTime==null)) {
		    	 list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.finished()
						.involvedUser(userId)
						.orderByProcessInstanceEndTime()
						.desc()
						.listPage(firstResult, maxResults);
		    } 
		    else if ((beginTime != null && beginTime.length()!= 0) 
			    	 &&(endTime != null && endTime.length()!= 0)) {
		    	Date dateBegin = simpleDateFormat.parse(beginTime);
			    Date dateEnd = simpleDateFormat.parse(endTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.finishedAfter(dateBegin)
						.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
						.involvedUser(userId)
						.orderByProcessInstanceEndTime()
						.desc()
						.listPage(firstResult, maxResults);
		    }
		    else if (beginTime != null && beginTime.length()!= 0){
		    	Date dateBegin = simpleDateFormat.parse(beginTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.finishedAfter(dateBegin)
						.involvedUser(userId)
						.orderByProcessInstanceEndTime()
						.desc()
						.listPage(firstResult, maxResults);
		    } 
		    else if (endTime != null && endTime.length()!= 0) {
		    	Date dateEnd = simpleDateFormat.parse(endTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
						.involvedUser(userId)
						.orderByProcessInstanceEndTime()
						.desc()
						.listPage(firstResult, maxResults);
		    }
			
			
			// 关联业务实体
			for (HistoricProcessInstance historicProcessInstance : list) {
				JSONObject jo = new JSONObject();
				String businessKey = historicProcessInstance.getBusinessKey();
				jo.put("businessKey", businessKey);
				// 设置属性
				JSONObject taskObject = new JSONObject();
				taskObject.put("historyId", historicProcessInstance.getId());
				taskObject.put("historyName", historicProcessInstance.getName());
				taskObject.put("processTime",historicProcessInstance.getEndTime());
				jo.put("history", taskObject);
				leaves.add(jo);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}

		return leaves;
		
		/**
		if (StringUtils.isNotBlank(beginTime) && StringUtils.isNotBlank(endTime)) {
			List<Map<String,Object>> leave2 = new ArrayList<>();
			try {
				SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
			    Date dateBegin;
			    Date dateEnd;
			    dateBegin = simpleDateFormat.parse(beginTime);
				dateEnd   = simpleDateFormat.parse(endTime);
				long beginTimeLong = dateBegin.getTime();
				long endTimeLong   = dateEnd.getTime()+24*60*60*1000;
				for (Map<String, Object> map : leaves) {
					JSONObject obj = (JSONObject)map.get("history");
					JSONObject obj2 = (JSONObject)obj.get("processTime");
					Long times = Long.parseLong(String.valueOf(obj2.get("time")));
					if (times>=beginTimeLong && times<=endTimeLong) {
						leave2.add(map);
					}	
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			return leave2;
		}
		*/
	}
	
	/**
	 * 查询指定用户数组参与已结束的流程实例
	 * 
	 * @param processDefinitionKey
	 * @return
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public List<Map<String, Object>> findFinishedProcessInstancesByUsers (
			String[] userIds, 
			int firstResult, 
			int maxResults,
			String beginTime,
			String endTime,
			String businessType) {
		List<HistoricProcessInstance> list = new ArrayList<HistoricProcessInstance>();
		List<Map<String, Object>> leaves = new ArrayList<Map<String, Object>>();
		SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
		try{
			for(int i = 0; i < userIds.length; i++){
				List<HistoricProcessInstance> tempList = null;
				// 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
			    if (("".equals(beginTime) || beginTime==null) && ("".equals(endTime) || endTime==null)) {
			    	tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.finished()
							.involvedUser(userIds[i])
							.orderByProcessInstanceEndTime()
							.desc().list();
							//.listPage(firstResult, maxResults);
			    } 
			    else if ((beginTime != null && beginTime.length()!= 0) 
				    	 &&(endTime != null && endTime.length()!= 0)) {
			    	Date dateBegin = simpleDateFormat.parse(beginTime);
				    Date dateEnd = simpleDateFormat.parse(endTime);
				    tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.finishedAfter(dateBegin)
							.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
							.involvedUser(userIds[i])
							.orderByProcessInstanceEndTime()
							.desc().list();
							//.listPage(firstResult, maxResults);
			    }
			    else if (beginTime != null && beginTime.length()!= 0){
			    	Date dateBegin = simpleDateFormat.parse(beginTime);
			    	tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.finishedAfter(dateBegin)
							.involvedUser(userIds[i])
							.orderByProcessInstanceEndTime()
							.desc().list();
							//.listPage(firstResult, maxResults);
			    } 
			    else if (endTime != null && endTime.length()!= 0) {
			    	Date dateEnd = simpleDateFormat.parse(endTime);
			    	tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
							.involvedUser(userIds[i])
							.orderByProcessInstanceEndTime()
							.desc().list();
							//.listPage(firstResult, maxResults);
			    } 
			    else {
			    	tempList = new ArrayList<HistoricProcessInstance>();
			    }
			    list.addAll(tempList);
			}	
			// 获取分页数据
			list = (list.size() > maxResults) ? list.subList(firstResult, maxResults) : list.subList(firstResult, list.size());
			// 关联业务实体
			for (HistoricProcessInstance historicProcessInstance : list) {
				JSONObject jo = new JSONObject();
				String businessKey = historicProcessInstance.getBusinessKey();
				jo.put("businessKey", businessKey);
				// 设置属性
				JSONObject taskObject = new JSONObject();
				taskObject.put("historyId", historicProcessInstance.getId());
				taskObject.put("historyName", historicProcessInstance.getName());
				taskObject.put("processTime",historicProcessInstance.getEndTime());
				jo.put("history", taskObject);
				leaves.add(jo);
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}

		return leaves;
	}

	@Override
	@Transactional(readOnly=true)
	public int findFinishedProcessInstancesByUser(String userId,String beginTime,
			String endTime,String businessType) {
		// TODO Auto-generated method stub
		// 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
		List<HistoricProcessInstance> list = null;
		try{
			SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
		    if (("".equals(beginTime) || beginTime==null) && ("".equals(endTime) || endTime==null)) {
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.finished()
						.involvedUser(userId)
						.list();
		    }
		    else if ((beginTime != null && beginTime.length()!= 0) 
			    	 &&(endTime != null && endTime.length()!= 0)) {
		    	Date dateBegin = simpleDateFormat.parse(beginTime);
			    Date dateEnd = simpleDateFormat.parse(endTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.involvedUser(userId)
						.finishedAfter(dateBegin)
						.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
						.list();
		    }
		    else if (beginTime != null && beginTime.length()!= 0){
		    	Date dateBegin = simpleDateFormat.parse(beginTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.involvedUser(userId)
						.finishedAfter(dateBegin)
						.list();
		    }
		    else if (endTime != null && endTime.length()!= 0) {
		    	Date dateEnd = simpleDateFormat.parse(endTime);
		    	list = historyService
						.createHistoricProcessInstanceQuery()
						.processDefinitionKey(businessType)
						.involvedUser(userId)
						.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
						.list();
		    }
		}catch(Exception e){
			e.printStackTrace();
		}
		return list.size();
		
		/**
		if (StringUtils.isNotBlank(beginTime) && StringUtils.isNotBlank(endTime)) {
			List<HistoricProcessInstance> leave2 = new ArrayList<>();
			try{
				SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
			    Date dateBegin;
			    Date dateEnd;
			    dateBegin = simpleDateFormat.parse(beginTime);
				dateEnd   = simpleDateFormat.parse(endTime);
				long beginTimeLong = dateBegin.getTime();
				long endTimeLong   = dateEnd.getTime()+24*60*60*1000;
				
				for (HistoricProcessInstance historicProcessInstance : list) {
					Date endTime2 = historicProcessInstance.getEndTime();
					long times = endTime2.getTime();
					if (times>=beginTimeLong && times<=endTimeLong) {
						leave2.add(historicProcessInstance);
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			return leave2.size();	
		}	
		*/
		
	}
	
	@Override
	@Transactional(readOnly=true)
	public int findFinishedProcessInstancesByUsers(String[] userIds,String beginTime,
			String endTime,String businessType) {
		// TODO Auto-generated method stub
		// 根据流程定义的key查询已经结束的流程实例(HistoricProcessInstance)
		List<HistoricProcessInstance> list = new ArrayList<HistoricProcessInstance>();
		try{
			SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd");
			for (int i = 0; i < userIds.length; i++){
				List<HistoricProcessInstance> tempList = null;
				if (("".equals(beginTime) || beginTime==null) && ("".equals(endTime) || endTime==null)) {
					tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.finished()
							.involvedUser(userIds[i])
							.list();
			    }
			    else if ((beginTime != null && beginTime.length()!= 0) 
				    	 &&(endTime != null && endTime.length()!= 0)) {
			    	Date dateBegin = simpleDateFormat.parse(beginTime);
				    Date dateEnd = simpleDateFormat.parse(endTime);
				    tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.involvedUser(userIds[i])
							.finishedAfter(dateBegin)
							.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
							.list();
			    }
			    else if (beginTime != null && beginTime.length()!= 0){
			    	Date dateBegin = simpleDateFormat.parse(beginTime);
			    	tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.involvedUser(userIds[i])
							.finishedAfter(dateBegin)
							.list();
			    }
			    else if (endTime != null && endTime.length()!= 0) {
			    	Date dateEnd = simpleDateFormat.parse(endTime);
			    	tempList = historyService
							.createHistoricProcessInstanceQuery()
							.processDefinitionKey(businessType)
							.involvedUser(userIds[i])
							.finishedBefore(new Date(dateEnd.getTime()+24*60*60*1000))
							.list();
			    }
			    else {
			    	tempList = new ArrayList<HistoricProcessInstance>();
			    }
				list.addAll(tempList);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return list.size();
		
	}

	/**
	 * 根据流程定义Id查询流程定义
	 */
	public ProcessDefinition getProcessDefinition(String processDefinitionId) {
		ProcessDefinition processDefinition = repositoryService
				.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		return processDefinition;
	}

	/**
	 * 根据任务Id查询任务
	 */
	public TaskEntity findTaskById(String taskId) throws Exception {
		TaskEntity task = (TaskEntity) taskService.createTaskQuery()
				.taskId(taskId).singleResult();
		if (task == null) {
			throw new Exception("任务实例未找到!");
		}
		return task;
	}

	

}