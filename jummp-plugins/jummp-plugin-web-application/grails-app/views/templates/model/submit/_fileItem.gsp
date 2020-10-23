<li class="media">
    <div class="media-body mb-1">
        <div class="row">
            <div class="columns small-12 medium-5 large-5">
                <p class="mb-2">
                    <strong class="file-name">${file.filename}</strong> - Size: <strong
                    class="file-size">${net.biomodels.jummp.utils.DisplayFormat.format(file.size, 2)}</strong>, Status: <span
                    class="text-muted">
                    <g:if test="${file?.size}">
                        Reload Complete
                    </g:if>
                    <g:else>
                        Reload Failed
                    </g:else>
                    </span>
                    <a
                    class="original-file-size" style="display: inline-block">${file.size}</a>
                </p>
                %{--<div class="progress mb-2">
                    <div class="progress progress-bar progress-bar-striped progress-bar-animated bg-primary"
                         role="progressbar"
                         style="width: 0%"
                         aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                    </div>
                </div>--}%
            </div>
            <div class="columns small-12 medium-6 large-6">
                <label>File Description
                    <input type="text" name="fileDescription" class="file-description" value="${file.description}"
                           placeholder="Describe this file. For example: What is this file used for?">
                </label>
            </div>
            <div class="columns small-12 medium-1 large-1">
                <label>Model file
                <g:if test="${file.isModelFile}">
                    <input type="checkbox" name="isModelFile" class="is-model-file" checked></label>
                </g:if>
                <g:else>
                    <input type="checkbox" name="isModelFile" class="is-model-file"></label>
                </g:else>
            </div>
        </div>
    </div>
    <button type="button" name="removeFile" class="button btn-remove-file">Remove</button>
    <hr style="color: lightgrey"/>
</li>
