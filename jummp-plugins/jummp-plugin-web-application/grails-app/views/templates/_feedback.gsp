<div class="reveal" id="rate_review_form" data-reveal>
    <h3 id="messageTitle"
        style="border-bottom: 1px solid grey">Please give your feedback</h3>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
    <div class="contact-panel" id="feedback_panel" data-toggler=".is-active">
        <table class="responsive-table">
            <jummp:renderRatingStars />
        </table>
        <form action="">
            <div class="row">
                <label>Email (Optional)
                    <input type="email" id="emailAddress" placeholder="Email address">
                </label>
                <label>Additional comments (Optional)
                    <textarea id="additionalComment"
                              placeholder="Share details of your experience with this site."
                              rows="3"></textarea>
                </label>
            </div>
            <div class="contact-panel-actions">
                <input id="submitButtonRate" type="submit"
                       class="button submit-button" value="Submit">
            </div>
        </form>
    </div>
</div>
