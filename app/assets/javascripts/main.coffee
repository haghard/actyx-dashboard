$ ->
  ws = new WebSocket $("body").data("ws-url")
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    insertNewAlert(message)
    showAlert(message)
    renderPieChart(message, filter)

  ws.onerror = (evt) ->
    alert "error " + evt

insertNewAlert = (json) ->
  article = $("#alerts").find("article:last").remove()
  $(article).find("#device").html "<ap>device: " + json.deviceId + "</ap>"
  $(article).find("#ma").html "<ap>ma: " + json.ma + "</ap>"
  $(article).find("#when").html "<ap>at: " + json.when + "</ap>"
  $(article).find("#actlimit").html "<ap>act/limit: " + json.actlimit + "</ap>"
  $("#alerts").find("article:first").before article

showAlert = (json) ->
  $('#alert').toastee({
    type: 'error',
    header: 'Power was exceeded',
    message: 'device: ' + json.deviceId + '\n at: ' + json.when + ' ma: ' + json.ma,
    width: 300
  })

draw = (js, source) ->
  pieHeight = 450
  pieWidth = 450
  pieChart = nv.models.pieChart()
      .x((json) -> json.key)
      .y((json) -> json.y)
      .width(pieWidth)
      .height(pieHeight)
      .showTooltipPercent(true)
      .showLabels(false)
  d3.select("#pie")
      .datum(source)
      .transition()
      .duration(1200)
      .attr('width', pieWidth)
      .attr('height', pieHeight)
      .call(pieChart)

renderPieChart = (js, filter) ->
  console.log("in", js.deviceId)
  if(filter.has(js.deviceId))
    for elem in source
      if elem.key == js.deviceId
        elem.y = elem.y + 1

    draw(js, source)