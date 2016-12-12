<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along 
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>











 <form id="local-search" name="local-search" action="${createLink(controller: 'search', action: 'searchRedir')}" method="post">
                
          <fieldset>
          
          <div class="left">
            <label>
            <input type="text" value="${query}" name="search_block_form" id="local-searchbox" />
            </label>
          </div>
          
          <div class="right">
            <input type="submit" name="submit" value="Search" class="submit">          
            <!-- If your search is more complex than just a keyword search, you can link to an Advanced Search,
                 with whatever features you want available 
            <span class="adv"><a href="../search" id="adv-search" title="Advanced">Advanced</a></span>-->
          </div>                  
          
          </fieldset>
          
        </form>
