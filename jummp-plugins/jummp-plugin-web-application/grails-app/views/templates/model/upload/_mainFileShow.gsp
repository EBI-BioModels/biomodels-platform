<tr class="prop">
    <td class="value" style="width: 20%">
        <span id="mainFile${index}" class="mainFile${index}">${name}</span>
        <input style="display: none;" type="file" id="mainFile"
               name="mainFile" class="mainFile${index}"/>
        <input style="display: none;" type="text" id="mainFileUpload"
               name="mainFileUpload" class="mainFile${index}" value="${name}"/>
    </td>
    <td style="width: 70%">
        <input type="text" id="mainFileDescription${index}"
               class="mainFileDescription${index}"
               name="mainFileDescription" value="${description}"
               placeholder="Please enter a description about the file type and format (e.g., SBML L2V4 representation of My Very Cool Model)"
               required>
    </td>
    <td style="width: 10%; text-align: center; vertical-align: middle">
        <a href="#" class="replaceMain">Replace</a> | <a href="#" class="removeMain">Remove</a>
    </td>
</tr>
