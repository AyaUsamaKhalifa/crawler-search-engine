import React from 'react';
import './SearchArea.css'
import TextField from '@mui/material/TextField';
import Autocomplete from '@mui/material/Autocomplete';
import CircularProgress from '@mui/material/CircularProgress';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { useNavigate } from 'react-router-dom';
import { Button } from '@mui/material';
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
  const [searchText, setSearchText] = React.useState();
  const [searchInput, setSearchInput] = React.useState("");
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
  const navigate = useNavigate();
  function handleClick(event)
  {
    
    if (searchText !== "" && searchText !== null && searchText !== undefined && typeof searchText !== 'string') {
      console.log(searchText.title);
      navigate(`/${searchText.title}`)
      axios.post("http://localhost:8081/PopularSearches", { title: `${searchText.title}` })
        .then(response => { console.log(response.data); })
        .catch(error => { console.log(error); });
        
    }
    else if (searchInput !== "" && searchInput !== null) { 
      console.log(searchInput)
      navigate(`/${searchInput}`)
      console.log(`/${searchInput}`)
      axios.post("http://localhost:8081/PopularSearches", { title: `${searchInput}` })
        .then(response => { console.log(response.data); })
        .catch(error => { console.log(error); });
        
    }
  }
  function handleChange(event, value) { 
    setSearchText(value)
  }
  function handleInput(event, value) { 
    setSearchInput(value)
  }
  function handleOption(option) { 
    if (typeof option === 'string') {
      return option;
    }
    else { 
      return option.title;
    }
  }
  

  return (
    <div className='search-area'>
      <div className='search-bar'>
      <ThemeProvider theme={theme} >
          <Autocomplete
              sx = {{ width: "100%", height: "100%",borderColor:"red" }}
              open = {open}
              onOpen = {() => {
              setOpen(true);
              }}
              onClose = {() => {
              setOpen(false);
              }}
              isOptionEqualToValue = {(option, value) => option.title === value.title}
              getOptionLabel = {handleOption}
              options={options}
              freeSolo
              filterOptions={filteredOptions}
              loading={loading}
              onChange={handleChange}
              onInputChange={handleInput}
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
        </div>
        <div className='button'>
        {((searchText != null && searchText.length) || searchInput.length) ? (<Button color='secondary' variant="outlined" onClick={handleClick}>Search</Button>)
            : (<Button color='secondary' variant="outlined" disabled>Search</Button>)}
        </div>
      </div>
  );
}

export default SearchArea;