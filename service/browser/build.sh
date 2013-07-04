rsync -av resources/common/images ../static/nlog/resources/common
rsync -av resources/themes/default/images ../static/nlog/resources/themes/default
rsync -av resources/i18n ../static/nlog/resources/themes

mkdir -p ../static/nlog/resources/themes/default/styles

node build nlog-debug.html ../static/nlog/index.html ../static/nlog/nlog.js ../static/nlog/resources/themes/default/styles/nlog.css release

