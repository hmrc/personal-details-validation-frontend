$(function() {
    //Accessibility
    var errorSummary =  $('#error-summary-display'),
    $input = $('input:text')
    //Error summary focus
    if (errorSummary){ errorSummary.focus() }
    $input.each( function(){
        if($(this).closest('label').hasClass('form-field--error')){
            $(this).attr('aria-invalid', true)
        }else{
            $(this).attr('aria-invalid', false)
        }
    });
    //Add aria-hidden to hidden inputs
    $('[type="hidden"]').attr("aria-hidden", true)

    var showHideContent = new GOVUK.ShowHideContent()
    showHideContent.init()
});