#!/usr/bin/env sh

# Space separated env variable names
keys="ESPER_API_ADDRESS"

for file in assets/config-*.js*; do
	for key in ${keys}; do
		# Get environment variable
		value=$(eval echo "\$$key")
		echo "replacing $key by $value"

		# replace __[variable_name]__ value with environment variable
		sed -i 's|__'"$key"'__|'"$value"'|g' $file
	done
done

http-server
