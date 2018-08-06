<tr class="fileEntry">
    <td class="name" style="width: 20%">${name}
    <input style="display: none" type="text" id="extraFiles${counter}"
           name="existedExtraFiles" value="${name}" readonly></td>
    <td class="name" style="width: 70%">
        <input name="existedExtraFileDescriptions" id="description${counter}"
               type="text" value="${description}"
               style="width: 100%; box-sizing: border-box; -webkit-box-sizing: border-box; -moz-box-sizing: border-box;"
               required></td>
    <td style="width: 10%; vertical-align: middle; text-align: center">
        <a href="#" class="killer" id="discardExtraFiles${counter}"
           title="Discard file">Discard</a></td>
</tr>
