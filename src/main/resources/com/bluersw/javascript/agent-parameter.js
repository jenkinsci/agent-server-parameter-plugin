jQuery.noConflict();

function bindOnChange(parent,requestBasicUrl){
	var selectE = parent.find('#agentParameterSelect');
	var messageD = parent.find('#result_message');
	selectE.change(function(){
		requestUrl = requestBasicUrl + "&value=" + jQuery(this).children('option:selected').val();
		jQuery.ajax({url:requestUrl,success:function(result){
			messageD.text(result);
			}})
		}
	)
}