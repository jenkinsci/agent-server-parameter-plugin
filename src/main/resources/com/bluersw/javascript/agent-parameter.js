Behaviour.specify(".agent-server-parameter", "agent-server-parameter", 0, function (container) {
	const select = container.querySelector(".agent-server-parameter-select");
	const message = container.querySelector(".agent-server-parameter-message");

	select.addEventListener("change", function () {
		const requestUrl = new URL(container.dataset.updateUrl, window.location.href);
		requestUrl.searchParams.set("name", container.dataset.parameterName);
		requestUrl.searchParams.set("value", select.value);

		fetch(requestUrl, {
			method: "POST",
			headers: crumb.wrap({Accept: "text/plain"}),
		})
			.then(function (response) {
				if (!response.ok) {
					throw new Error(response.statusText);
				}
				return response.text();
			})
			.then(function (result) {
				message.textContent = result;
			})
			.catch(function (error) {
				message.textContent = error.message;
			});
	});
});
