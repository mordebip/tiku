[#ftl][#setting locale="fi"]
<!DOCTYPE html>
<html lang="${lang}">
[#function message code]
    [#if rc.getMessage(code)??]
        [#return rc.getMessage(code, "[${code}]") /]
    [/#if]
    [#return "- ${code} - " /]
[/#function]
[#macro label e][#if e?? && e.label??]${(e.label.getValue(lang)!"n/a")?json_string}[#else]???[/#if][/#macro]
[#macro breadcrumb e leaf = false]
  [#if e.parent??]
    [@breadcrumb e.parent /]
  [/#if]
  [#if leaf]
    <li>[@label e /]</li>
  [#else]
    <li><a href="${rc.contextPath}/${env}/${lang}/${subject}/${hydra}/${cube}/${e.surrogateId}">[@label e /]</a></li>
  [/#if]
[/#macro]
[#if nodes??]
[#list nodes as node]
<head>

  <title>[@label node /] - [#if cubeLabel??]${cubeLabel.getValue(lang)}[#else]n/a[/#if] - ${message("site.title")}</title>
  <link rel="stylesheet" href="${rc.contextPath}/resources/css/bootstrap.min.css" />
  <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Source+Sans+Pro:400,400italic,600,600italic,700,700italic" />
  <link rel="stylesheet" href="${rc.contextPath}/resources/css/style.css" />

  <style>
    h1 { margin-left: 0px; border-bottom: 1px solid #ccc; }
    h2 { font-size: 24px; }
    dd { margin-left: 15px; margin-bottom: 15px;}
  </style>
  <!--[if lt IE 9]>
    <script src="${rc.contextPath}/resources/js/html5shiv.js"></script>
    <script src="${rc.contextPath}/resources/js/respond.min.js"></script>
    <![endif]-->
</head>
<body>
  <div class="container">

    <h1>[@label node /]</h1>
    <ul class="breadcrumb">
      [@breadcrumb node true /]
    </ul>

    [#if node.getProperty("meta:description")?? || node.getProperty("meta:comment")??]
      <h2>Kuvaus</h2>
      [#if node.getProperty("meta:description")??]
        <p>
          ${node.getProperty("meta:description").getValue(lang)!}
        </p>
      [/#if]
      [#if node.getProperty("meta:comment")??]
        <p>
          ${node.getProperty("meta:comment").getValue("fi")!}
        </p>
      [/#if]
    [/#if]
    <h2>Tekniset metatiedot</h2>
    <dl>
      <dt>Dimensio</dt>
      <dd>[@label node.dimension /] / [@label node.level /]</dd>
      <dt>Tunniste<dt>
      <dd>${node.id}</dd>
      <dt>Koodiarvo</dt>
      <dd>${node.code}</dd>
      <dt>URI</dt>
      <dd>${node.reference}</dd>
    </dl>


    [#if node.children?size > 0]
    <h2>Alakäsitteet</h2>
      <ul>
        [#list node.children as child]
        <li><a href="${rc.contextPath}/${env}/${lang}/${subject}/${hydra}/${cube}/${child.surrogateId}">[@label child /]</a></li>
        [/#list]
      </ul>
    [/#if]

  </div>

</body>
[/#list]
[/#if]
</html>
