<div class="row">
    <div class="small-12 medium-6 large-4 columns">
        Tags
    </div>
    <div class="small-12 medium-6 large-8 columns">
        <select class="model-tags-select2" name="tags" multiple="multiple">
        <g:each in="${bmTags}" var="bmTag">
            <option class="model-tag" value="${bmTag?.id}" id="${bmTag?.id}" name="${bmTag?.name}" selected>${bmTag?.name}</option>
        </g:each>
        <g:each in="${unTags}" var="bmTag">
            <option class="model-tag" value="${bmTag?.id}" id="${bmTag?.id}" name="${bmTag?.name}">${bmTag?.name}</option>
        </g:each>
        </select>
        <br/>
        <button type="button" class="button float-right" id="btnSaveTags"
                style="border-radius: 5px; margin: 2px 2px 2px 0">Save</button>
    </div>
</div>
