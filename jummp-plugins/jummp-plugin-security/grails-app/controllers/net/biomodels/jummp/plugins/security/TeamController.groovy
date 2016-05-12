/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp.plugins.security
import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import net.biomodels.jummp.core.adapters.DomainAdapter

/**
 * @short Controller class for interacting with user teams.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Secured(["isAuthenticated()"])
class TeamController {
    static allowedMethods = [update: "POST"]
    /**
     * Dependency Injection of Spring Security Service
     */
    def springSecurityService
    /**
     * Dependency Injection of Team Service
     */
    def teamService

    /**
     * Renders the form to create new teams.
     */
    def create() {
        render view: "create", model: [teamOwner: springSecurityService.getCurrentUser()]
    }

    def save() {
    	String name="";
    	String description="";
    	Set<User> users=new HashSet<User>();
    	try {
    		def map = JSON.parse(params.teamData);
    		name = map.getString("name");
    		description = map.getString("description");
    		def collabs = map.getJSONArray("members");
    		for (int i = 0; i < collabs.length(); i++) {
    			users.add(User.findByUsername(collabs.getJSONObject(i).getString("userId")));
    		}
    	}
    	catch(Exception e) {
    		render "Error processing parameters: "+e.getMessage();
    		return;
    	}
    	def team = new Team(name: name, description: description)
    	team.owner=springSecurityService.getCurrentUser();
        User currentUser = users.find { it.getId() == team.owner.id }
        if (!currentUser) {
            // By default, the owner/creator/current user should be added to the team automatically
            users.add(team.owner)
        }
    	if (!team.validate()) {
            render "Error creating team. Team could not be validated."
        }
        else {
        	team.save(flush: true)
        	users.each {
        		UserTeam.create(it, team)
        	}
        	render team.id
        }
    }

    void showStandardErrorMessage() {
    	flash.message = "Could not find that team. Please select one from the list below."
        redirect(action: 'index')
    }
    
    /**
     * Lists the teams belonging to the current user.
     */
    def index() {
        def user = springSecurityService.getCurrentUser()
        [teams: teamService.getTeamsForUser(user)]
    }

    def edit(Long id) {
    	if (!id) {
    		showStandardErrorMessage()
    	}
    	else {
    		Team team = Team.get(id)
    		def user = springSecurityService.getCurrentUser()
    		if (!team || team.owner != user) {
    			showStandardErrorMessage()
    		}
    		else {
    			def usersInTeam = UserTeam.findAllByTeam(team);
    			[team: team, users: usersInTeam.collect { [name: it.user.person.userRealName, userId: it.user.username, id: it.user.id] } as JSON]
    		}
    	}
    }

    def delete(Long id) {
        if (id <= 0 || id == null) {
            showStandardErrorMessage()
        } else {
            try {
                log.info("Team existing.")
                boolean deleted = teamService.deleteTeam(id)
                if (deleted) {
                    flash.message = "The team has been deleted successfully."
                } else {
                    flash.message = "There is an error to delete the team."
                }
            } catch (Exception e) {
                flash.message = "Cannot delete the team ${id}"
            }
            redirect(action: "index")
        }
    }

    def update(Long id) {
    	if (!id) {
    		showStandardErrorMessage()
    	}
    	else {
    		Team team = Team.get(id)
    		def user = springSecurityService.getCurrentUser()
    		if (team && user == team.owner) {
    			String name="";
    			String description="";
    			Set<User> users=new HashSet<User>();
    			try {
    				def map = JSON.parse(params.teamData);
    				team.name = map.getString("name");
    				team.description = map.getString("description");
    				def collabs = map.getJSONArray("members");
    				for (int i = 0; i < collabs.length(); i++) {
    					users.add(User.findByUsername(collabs.getJSONObject(i).getString("userId")));
    				}
    			}
    			catch(Exception e) {
    				render "Error processing parameters: "+e.getMessage();
    				return;
    			}
    			if (!team.validate()) {
    				render "Error updating team. Team could not be validated."
    			}
    			else {
    				team.save(flush: true)
    				Set<User> existingUsers = UserTeam.findAllByTeam(team).collect{ it.user };
    				Set<User> newUsers = users - existingUsers;
    				newUsers.each {
    					UserTeam.create(it, team)
    				}
    				Set<User> removeThese = existingUsers - users;
    				removeThese.each {
    					UserTeam userTeam = UserTeam.findByUserAndTeam(it, team)
    					userTeam.delete();
    				}
    				render team.id
    			}
    		}
    		else {
    			showStandardErrorMessage()
    		}
        }
    }

    // TODO secure this action to ensure that the user has access to the team being accessed
    def show(Long id) {
        Team team = Team.get(id)
        if (!team) {
            showStandardErrorMessage();
        }
        else {
        	def usersInTeam = UserTeam.findAllByTeam(team);
        	[team: team, users: usersInTeam.collect { DomainAdapter.getAdapter(it.user.person).toCommandObject()}]
        }
    }
}
