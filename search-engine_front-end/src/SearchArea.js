import React from 'react';
import './home.css';
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import axios from 'axios';


function filteredOptions(options, { inputValue, input }) { 
  const filteredOptions = options.filter(option =>
    option.title.toLowerCase().includes(inputValue.toLowerCase())
  );
  return filteredOptions.slice(0, 5);
}

function SearchArea() {
  const [open, setOpen] = React.useState(false); 
  const [options, setOptions] = React.useState([]);
  const loading = open && options.length === 0;
  React.useEffect(() => {

    if (!loading) {
      return undefined;
    }

    (async () => {

      await axios.get("http://localhost:8081/PopularSearches")
        .then(response => {
        setOptions(response.data);

      }).catch(error => {
        console.error(error);
      });
    })();

  }, [loading]);

  React.useEffect(() => {
    if (!open) {
      setOptions([]);
    }
  }, [open]);

  const theme = createTheme({
    palette: {
      primary: {
        main: '#CB6CE6 !important',
      },
    },
  });

  return (
    <ThemeProvider theme={theme} >
        <Autocomplete
            sx = {{ width: "100%",borderColor:"red" }}
            open = {open}
            onOpen = {() => {
            setOpen(true);
            }}
            onClose = {() => {
            setOpen(false);
            }}
            isOptionEqualToValue = {(option, value) => option.title === value.title}
            getOptionLabel = {(option) => option.title}
            options={options}
            filterOptions={filteredOptions}
            loading={loading}
            renderInput = {(params) => (
            <TextField
                {...params}
                
                label="Enter your search here..."
                InputProps = {{
                ...params.InputProps,
                sx: {color: '#9700C2'},
                endAdornment: (
                    <React.Fragment>
                    {loading ? <CircularProgress color="inherit" size={20} /> : null}
                    {params.InputProps.endAdornment}
                    </React.Fragment>
                ),
                }}
            />
            )}
        />    
    </ThemeProvider>
  );
}

export default SearchArea;