function fn() {
  var env = karate.env || 'local';
  var config = {
    baseUrl: 'http://localhost:8080'
  };

  if (env === 'ci') {
    config.baseUrl = karate.properties['baseUrl'] || 'http://localhost:8080';
  } else if (karate.properties['baseUrl']) {
    config.baseUrl = karate.properties['baseUrl'];
  }

  return config;
}
