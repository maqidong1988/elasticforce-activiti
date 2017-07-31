package org.activiti.web.simple.webapp.model.example;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.activiti.web.simple.webapp.model.Group;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 表单实体
 * 
 * @author Administrator
 *
 */
public class User implements Serializable {

	private String firstName;

	private String lastName;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
	private Date birthday;
	
	private List<Group> actIdGroups;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getBirthday() {
		return birthday;
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	
	//bi-directional many-to-many association to Group
    @ManyToMany
    @JoinTable(name = "ACT_ID_MEMBERSHIP", joinColumns = {@JoinColumn(name = "USER_ID_")}, inverseJoinColumns = {@JoinColumn(name = "GROUP_ID_")})
    public List<Group> getActIdGroups() {
        return this.actIdGroups;
    }

    public void setActIdGroups(List<Group> actIdGroups) {
        this.actIdGroups = actIdGroups;
    }

}